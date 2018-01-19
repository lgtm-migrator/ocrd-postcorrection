package ocrd.rest.raml.impl;

import de.lmu.cis.pocoweb.Client;
import org.raml.jaxrs.example.model.Projects;
import org.raml.jaxrs.example.model.UploadProjectData;
import org.raml.jaxrs.example.resource.ProjectsResource;

import ocrd.rest.raml.handler.ProjectsHandler;

public class ProjectsResourceImpl implements ProjectsResource {

  @Override
  public GetProjectsListResponse getProjectsList() throws Exception {
    try (Client client = newClient();) {
      return GetProjectsListResponse.withJsonOK(client.listProjects());
    }
  }

  @Override
  public GetProjectsByProjectIDPagesByPageIDResponse
  getProjectsByProjectIDPagesByPageID(String projectID, String pageID)
      throws Exception {
    throw new Exception("Not implemented");
  }

  @Override
  public GetProjectsByProjectIDResponse getProjectsByProjectID(String projectID) throws Exception {
  try (Client client = newClient();) {
      return GetProjectsByProjectIDResponse.withJsonOK(client.getProject(Integer.parseInt(projectID)));
  	}
  }
  @Override
  public PostProjectsByProjectIDAddBookResponse
  postProjectsByProjectIDAddBook(String projectID, UploadProjectData data)
      throws Exception {
    throw new Exception("Not implemented");
  }
  @Override
  public PostProjectsCreateResponse postProjectsCreate(UploadProjectData data) throws Exception {
    try (Client client = newClient();) {
    	System.out.println(data.getContent());
    }
	return null;
  }

  private static Client newClient() throws Exception {
    return Client.login("http://pocoweb.cis.lmu.de/rest", "pocoweb",
                        "pocoweb123");
  }
}
