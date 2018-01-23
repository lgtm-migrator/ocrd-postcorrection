package de.lmu.cis.pocoweb;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.commons.io.IOUtils;
import org.raml.jaxrs.example.model.Book;
import org.raml.jaxrs.example.model.Languages;
import org.raml.jaxrs.example.model.Page;
import org.raml.jaxrs.example.model.Project;
import org.raml.jaxrs.example.model.Projects;

public class Client implements AutoCloseable {
  private final String host;
  private String sid;
  public final static String LocalProfiler = "local";

  public static Client login(String host, String user, String pass)
      throws Exception {
    Client Client = new Client(host);
    return Client.login(user, pass);
  }

  public void close() throws Exception { logout(); }

  public Languages getProfilerLanguages(String profilerUrl) throws Exception {
    return get(String.format("/profiler-languages&url=%s",
                             URLEncoder.encode(profilerUrl, "UTF-8")),
               Languages.class);
  }
  public Languages getLocalProfilerLanguages() throws Exception {
    return getProfilerLanguages(LocalProfiler);
  }

  private class Books { public ProjectBook[] books; }
  private List<ProjectBook> listBooks() throws Exception {
    ProjectBook[] books = get("/books", Books.class, 200).books;
    List<ProjectBook> list = new ArrayList<ProjectBook>(books.length);
    for (ProjectBook b : books) {
      list.add(b);
    }
    return list;
  }

  public Projects listProjects() throws Exception {
    List<ProjectBook> projectBooks = listBooks();
    Map<Integer, Project> map = new HashMap<Integer, Project>();
    // System.out.println("got " + projectBooks.size() + " books");
    for (ProjectBook book : projectBooks) {
      Integer ocrId = book.getOcrId();
      if (!map.containsKey(ocrId)) {
        map.put(ocrId, book.newProject());
      } else {
        map.get(ocrId).getBooks().add(book.newBook());
      }
    }
    List<Project> projects = new ArrayList<Project>(map.size());
    // System.out.println("got " + map.size() + " projects");
    projects.addAll(map.values());
    return new Projects().withProjects(projects);
  }

  public Project getProject(int pid) throws Exception {
    for (Project p : listProjects().getProjects()) {
      if (p.getProjectId() == pid) {
        return p;
      }
    }
    throw new Exception("no such project: " + pid);
  }

  private ProjectBook getBook(int pid) throws Exception {
    return new ProjectBook(get("/books/" + pid, Book.class, 200));
  }

  public Project newProject(Book book, InputStream in) throws Exception {
    ProjectBook pbook = new ProjectBook(book);
    ProjectBook tmp =
        post("/books", in, ProjectBook.class, "application/zip", 200, 201);
    pbook.pageIds = tmp.pageIds;
    pbook.projectId = tmp.projectId;
    pbook.setOcrId(pbook.projectId); // ocrid is the first book's project id
    updateBookData(pbook);
    return pbook.newProject();
  }

  public Project addBook(Project project, Book book, InputStream in)
      throws Exception {
    ProjectBook pbook = new ProjectBook(book);
    ProjectBook tmp =
        post("/books", in, ProjectBook.class, "application/zip", 200, 201);
    pbook.pageIds = tmp.pageIds;
    pbook.projectId = tmp.projectId;
    updateBookData(pbook);
    project.getBooks().add(pbook.newBook());
    return project;
  }

  private ProjectBook updateBookData(ProjectBook p) throws Exception {
    return post(String.format("/books/%d", p.projectId), p, ProjectBook.class,
                200);
  }

  private void deleteBook(int bid) throws Exception {
    delete(String.format("/books/%d", bid), 200);
  }

  public void deleteProject(Project p) throws Exception {
    for (Book book : p.getBooks()) {
      deleteBook(book.getProjectId());
    }
  }

  public void orderProfile(int bid) throws Exception {
    HttpURLConnection con =
        getConnection(String.format("/books/%d/profile", bid), "POST");
    validateResponseCode(con.getResponseCode(), 202);
  }

  public int getProfilingStatus(int bid) throws Exception {
    HttpURLConnection con =
        getConnection(String.format("/books/%d/profile", bid), "GET");
    int rc = con.getResponseCode();
    validateResponseCode(rc, 200, 201, 202);
    return rc;
  }

  public Page getPage(int bid, int pid) throws Exception {
    return get(String.format("/books/%d/pages/%d", bid, pid), Page.class, 200);
  }

  private class Tokens { public Token[] tokens; }
  public List<Token> getTokens(int bid, int pid, int lid) throws Exception {
    Token[] tokens =
        get(String.format("/books/%d/pages/%d/lines/%d/tokens", bid, pid, lid),
            Tokens.class, 200)
            .tokens;
    List<Token> list = new ArrayList<Token>(tokens.length);
    for (Token t : tokens) {
      list.add(t);
    }
    return list;
  }

  public SuggestionsData getSuggestions(int pid) throws Exception {
    return get("/books/" + pid + "/suggestions", SuggestionsData.class, 200);
  }

  public String getHost() { return this.host; }
  public String getSid() { return this.sid; }

  private Client(String host) {
    this.host = host;
    this.sid = null;
  }

  private void logout() throws Exception {
    HttpURLConnection con = getConnection("/logout", "GET");
    validateResponseCode(con.getResponseCode(), 200);
  }

  private Client login(String user, String pass) throws Exception {
    SidData sid = post("/login", new LoginData(user, pass), SidData.class, 200);
    this.sid = sid.sid;
    return this;
  }

  private <T> T post(String path, Object data, Class<T> clss, int... codes)
      throws Exception {
    return post(path, IOUtils.toInputStream(new Gson().toJson(data), "UTF-8"),
                clss, "application/json; charset=UTF-8", codes);
  }

  private <T> T post(String path, InputStream in, Class<T> clss, String ct,
                     int... codes) throws Exception {
    HttpURLConnection con = getConnection(path, "POST");
    con.setRequestProperty("Content-Type", ct);
    con.setDoOutput(true);
    try (DataOutputStream out = new DataOutputStream(con.getOutputStream());) {
      IOUtils.copy(in, out);
      out.flush();
    }
    validateResponseCode(con.getResponseCode(), codes);
    try (InputStream cin = con.getInputStream();) {
      return deserialize(cin, clss);
    }
  }

  private <T> T get(String path, Class<T> clss, int... codes) throws Exception {
    HttpURLConnection con = getConnection(path, "GET");
    con.setRequestMethod("GET");
    validateResponseCode(con.getResponseCode(), codes);
    try (InputStream in = con.getInputStream();) {
      return deserialize(in, clss);
    }
  }

  private int delete(String path, int... codes)throws Exception {
    HttpURLConnection con = getConnection(path, "DELETE");
    validateResponseCode(con.getResponseCode(), codes);
    return con.getResponseCode();
  }

  private static <T> T deserialize(InputStream in, Class<T> clss)
      throws Exception {
    StringWriter out = new StringWriter();
    IOUtils.copy(in, out, Charset.forName("UTF-8"));
    // System.out.println("[Client] deserializing JSON: " + out.toString());
    return new Gson().fromJson(out.toString(), clss);
  }

  private static void validateResponseCode(int got, int... codes)
      throws Exception {
    System.out.println("[Client] got return code: " + got);
    for (int want : codes) {
      if (want == got) {
        return;
      }
    }
    throw new Exception("Invalid return code: " + got);
  }

  private HttpURLConnection getConnection(String path, String method)
      throws Exception {
    HttpURLConnection con =
        (HttpURLConnection) new URL(this.host + path).openConnection();
    if (this.sid != null) {
      con.setRequestProperty("Authorization", "Pocoweb " + sid);
    }
    con.setRequestMethod(method);
    System.out.println(
        String.format("[Client] sending request to [%s] %s", method, path));
    return con;
  }
}
