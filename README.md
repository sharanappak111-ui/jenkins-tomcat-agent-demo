# Jenkins Controller-Agent + Tomcat Multi-Stage Demo

This project demonstrates:
- A dedicated Jenkins agent selected by label
- Build parameters (`DEPLOY_ENV`, `RUN_TESTS`, `DEPLOY_TO_TOMCAT`, `APP_VERSION`)
- Pipeline options (`timestamps`, `timeout`, `buildDiscarder`, `disableConcurrentBuilds`)
- Parallel stages
- Maven unit tests and WAR packaging
- Archived build artifacts
- Conditional Tomcat deployment
- `post` actions for success, unstable, failure, aborted, and always

## 1. Agent architecture

The Jenkins controller schedules work. The dedicated agent runs shell commands, Maven, tests, packaging, and (optionally) local Tomcat deployment.

Create a separate Linux VM/EC2 instance for the agent if possible. Install:
- Java JDK 11 or newer (Jenkins controller and agent Java requirements can differ by Jenkins version; use a supported Java version for your Jenkins release)
- Git
- Maven 3.8+
- Optional: Apache Tomcat 9 if you want to try deployment

On the agent, confirm `java -version`, `git --version`, and `mvn -version` work for the account Jenkins uses.

## 2. Create and label the agent in Jenkins

1. In Jenkins, go to **Manage Jenkins → Nodes** (sometimes shown as **Manage Nodes and Clouds**).
2. Select **New Node**; name it `linux-maven-tomcat`; choose **Permanent Agent**.
3. Set **Remote root directory** to a writable path such as `/home/jenkins/agent`.
4. Set **Labels** to exactly `linux-maven-tomcat`.
5. Set **Usage** to **Only build jobs with label expressions** if you want to reserve it for explicitly labelled jobs.
6. Set an appropriate number of executors (1 is easiest for a demo).
7. Select the launch method supported by your Jenkins setup. Common options are SSH launch (requires SSH access and configured credentials) or inbound agent (agent connects to controller). Follow your Jenkins version's displayed instructions.
8. Save and complete the launch configuration. Wait until the node shows **Online**.

Security/network notes:
- Prefer a separate agent VM and a dedicated non-root OS account.
- Do not expose the Jenkins agent port publicly; use a private network/VPN/security-group rules.
- For SSH launch, use Jenkins Credentials for the SSH key. For inbound agents, use the exact command/secret shown by Jenkins; never paste secrets into source control.
- If the agent is offline, this pipeline will stay queued because it requires the `linux-maven-tomcat` label.

## 3. Put the project in Git

Commit all project files, including `Jenkinsfile`, to a Git repository. Ensure the Jenkins controller can reach that repository. Configure credentials in Jenkins if it is private.

## 4. Create the Pipeline job

1. Select **New Item → Pipeline**.
2. Under **Pipeline**, choose **Pipeline script from SCM**.
3. SCM: **Git**; enter repository URL and branch (e.g. `*/main`).
4. Script Path: `Jenkinsfile`.
5. Save, then run **Build Now**. After the first run, use **Build with Parameters** to choose inputs.

## 5. Parameters to demonstrate

- `DEPLOY_ENV`: `dev`, `test`, or `prod` label for the demo. It does not select a remote host; deployment, if enabled, is to Tomcat on the same agent.
- `RUN_TESTS`: toggles the unit-test parallel stage.
- `DEPLOY_TO_TOMCAT`: defaults to false. Set true only after installing Tomcat on the agent and configuring its webapps path and permissions.
- `APP_VERSION`: printed as a demo label; it does not change the Maven artifact version.

## 6. Optional Tomcat deployment

This is a local-to-agent demo deployment. It copies `target/demo-webapp.war` to `/opt/tomcat/webapps/demo-webapp.war`.

1. Install and run Tomcat 9 on the agent (this sample uses a simple JSP web app).
2. Set `TOMCAT_WEBAPPS_DIR` in the Jenkinsfile to the actual Tomcat `webapps` directory, often `/var/lib/tomcat9/webapps` or `/opt/tomcat/webapps`, depending on installation.
3. Give the Jenkins agent user only the necessary write permission to that directory. Avoid running Jenkins as root.
4. Run **Build with Parameters**, set `DEPLOY_TO_TOMCAT=true`.
5. After Tomcat deploys the WAR, open `http://<agent-host>:8080/demo-webapp/` if network access and Tomcat configuration permit it.

The pipeline does not stop/restart Tomcat or deploy to a remote server. For production, use a controlled deployment mechanism, credentials, approvals, and environment-specific targets.

## 7. Demonstration ideas

- Successful run: all checks, tests, and packaging pass.
- Parallel view: show the three child stages under **Parallel Checks**.
- Test failure: temporarily change an expected string in `MessageServiceTest.java`, commit, and rebuild. Restore the test afterwards.
- Parameter behaviour: set `RUN_TESTS=false`, then compare the skipped test stage.
- Deployment guard: leave `DEPLOY_TO_TOMCAT=false` for a safe first run; enable only after Tomcat is installed and writable.
- Post actions: review the final console messages after success/failure.

## Notes

- This is a learning/demo project, not a production deployment template.
- The pipeline requires a Unix/Linux agent with `sh`, Java, Git, Maven, and `tar`.
- The parallel stages run on the same allocated agent; the agent needs enough executors/resources for parallel work. If only one executor is configured, parallel branches may not execute concurrently in practice.
