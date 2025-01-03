const { execSync } = require("child_process");

try {
  // Run the container in detached mode and capture its ID
  console.log("Starting the container...");
  const containerId = execSync("docker run -d sweethome3d-build")
    .toString()
    .trim();

  // Wait for the container to stop
  console.log(`Waiting for container ${containerId} to finish...`);
  execSync(`docker wait ${containerId}`);

  // Copy build artifacts 
  /* Note that the ant cmd does stuff in /build and then cleans stuff up.  
     Since docker cp doesn't interpret wildcards and the jar name changes based on build version, 
     it's not trivial to get the jar file from the docker install folder and I don't want to update
     files that are tracked in git.  Easy solution is to copy the entire /workspace/install folder 
     to ./build and git ignore the build folder.
    */
  console.log("Copying build artifacts...");
  execSync(`docker cp ${containerId}:/workspace/install ./build`);

  // Remove the container
  console.log("Removing the container...");
  execSync(`docker rm -f ${containerId}`);

  console.log("Build completed successfully. Artifacts are in the ./build directory.");
} catch (err) {
  console.error("An error occurred:", err.message);
  process.exit(1);
}
