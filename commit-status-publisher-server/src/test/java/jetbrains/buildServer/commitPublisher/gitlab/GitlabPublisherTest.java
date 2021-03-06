package jetbrains.buildServer.commitPublisher.gitlab;

import com.google.gson.Gson;
import jetbrains.buildServer.commitPublisher.Constants;
import jetbrains.buildServer.commitPublisher.HttpPublisherTest;
import jetbrains.buildServer.commitPublisher.MockPluginDescriptor;
import jetbrains.buildServer.commitPublisher.PublisherException;
import jetbrains.buildServer.commitPublisher.gitlab.data.GitLabPermissions;
import jetbrains.buildServer.commitPublisher.gitlab.data.GitLabProjectAccess;
import jetbrains.buildServer.commitPublisher.gitlab.data.GitLabRepoInfo;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildRevision;
import jetbrains.buildServer.vcs.VcsRootInstance;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author anton.zamolotskikh, 05/10/16.
 */
@Test
public class GitlabPublisherTest extends HttpPublisherTest {

  public GitlabPublisherTest() {
    myExpectedRegExps.put(EventToTest.QUEUED, null); // not to be tested
    myExpectedRegExps.put(EventToTest.REMOVED, null); // not to be tested
    myExpectedRegExps.put(EventToTest.STARTED, String.format(".*/projects/owner%%2Fproject/statuses/%s.*ENTITY:.*running.*Build started.*", REVISION));
    myExpectedRegExps.put(EventToTest.FINISHED, String.format(".*/projects/owner%%2Fproject/statuses/%s.*ENTITY:.*success.*Success.*", REVISION));
    myExpectedRegExps.put(EventToTest.FAILED, String.format(".*/projects/owner%%2Fproject/statuses/%s.*ENTITY:.*failed.*Failure.*", REVISION));
    myExpectedRegExps.put(EventToTest.COMMENTED_SUCCESS, null); // not to be tested
    myExpectedRegExps.put(EventToTest.COMMENTED_FAILED, null); // not to be tested
    myExpectedRegExps.put(EventToTest.COMMENTED_INPROGRESS, null); // not to be tested
    myExpectedRegExps.put(EventToTest.COMMENTED_INPROGRESS_FAILED, null); // not to be tested
    myExpectedRegExps.put(EventToTest.INTERRUPTED, String.format(".*/projects/owner%%2Fproject/statuses/%s.*ENTITY:.*canceled.*%s.*", REVISION, PROBLEM_DESCR));
    myExpectedRegExps.put(EventToTest.FAILURE_DETECTED, String.format(".*/projects/owner%%2Fproject/statuses/%s.*ENTITY:.*failed.*%s.*", REVISION, PROBLEM_DESCR));
    myExpectedRegExps.put(EventToTest.MARKED_SUCCESSFUL, String.format(".*/projects/owner%%2Fproject/statuses/%s.*ENTITY:.*success.*marked as successful.*", REVISION));
    myExpectedRegExps.put(EventToTest.MARKED_RUNNING_SUCCESSFUL, String.format(".*/projects/owner%%2Fproject/statuses/%s.*ENTITY:.*running.*Build marked as successful.*", REVISION));
    myExpectedRegExps.put(EventToTest.TEST_CONNECTION, ".*/projects/owner%2Fproject .*");
  }

  public void should_fail_with_error_on_wrong_vcs_url() throws InterruptedException {
    myVcsRoot.setProperties(Collections.singletonMap("url", "wrong://url.com"));
    VcsRootInstance vcsRootInstance = myBuildType.getVcsRootInstanceForParent(myVcsRoot);
    BuildRevision revision = new BuildRevision(vcsRootInstance, REVISION, "", REVISION);
    try {
      myPublisher.buildFinished(myFixture.createBuild(myBuildType, Status.NORMAL), revision);
      fail("PublishError exception expected");
    } catch(PublisherException ex) {
      then(ex.getMessage()).matches("Cannot parse.*" + myVcsRoot.getName() + ".*");
    }
  }

  public void should_work_with_dots_in_id() throws PublisherException, InterruptedException {
    myVcsRoot.setProperties(Collections.singletonMap("url", "https://url.com/own.er/pro.ject"));
    VcsRootInstance vcsRootInstance = myBuildType.getVcsRootInstanceForParent(myVcsRoot);
    BuildRevision revision = new BuildRevision(vcsRootInstance, REVISION, "", REVISION);
    myPublisher.buildFinished(myFixture.createBuild(myBuildType, Status.NORMAL), revision);
    then(waitForRequest()).isNotNull().matches(String.format(".*/projects/own%%2Eer%%2Fpro%%2Eject/statuses/%s.*ENTITY:.*success.*Success.*", REVISION));
  }

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myPublisherSettings = new GitlabSettings(myExecServices, new MockPluginDescriptor(), myWebLinks, myProblems);
    Map<String, String> params = getPublisherParams();
    myPublisher = new GitlabPublisher(myPublisherSettings, myBuildType, FEATURE_ID, myExecServices, myWebLinks, params, myProblems);
  }

  @Override
  protected Map<String, String> getPublisherParams() {
    return new HashMap<String, String>() {{
      put(Constants.GITLAB_TOKEN, "TOKEN");
      put(Constants.GITLAB_API_URL, getServerUrl());
    }};
  }

  @Override
  protected void populateResponse(HttpRequest httpRequest, HttpResponse httpResponse) {
    super.populateResponse(httpRequest, httpResponse);
    if (httpRequest.getRequestLine().getMethod().equals("GET")) {
      if (httpRequest.getRequestLine().getUri().contains("/projects" +  "/" + OWNER + "%2F" + CORRECT_REPO)) {
        respondWithRepoInfo(httpResponse, CORRECT_REPO, true);
      } else if (httpRequest.getRequestLine().getUri().contains("/projects"  + "/" + OWNER + "%2F" +  READ_ONLY_REPO)) {
        respondWithRepoInfo(httpResponse, READ_ONLY_REPO, false);
      }
    }
  }

  private void respondWithRepoInfo(HttpResponse httpResponse, String repoName, boolean isPushPermitted) {
    Gson gson = new Gson();
    GitLabRepoInfo repoInfo = new GitLabRepoInfo();
    repoInfo.id = "111";
    repoInfo.permissions = new GitLabPermissions();
    repoInfo.permissions.project_access = new GitLabProjectAccess();
    repoInfo.permissions.project_access.access_level = isPushPermitted ? 30 : 20;
    String jsonResponse = gson.toJson(repoInfo);
    httpResponse.setEntity(new StringEntity(jsonResponse, "UTF-8"));
  }

}