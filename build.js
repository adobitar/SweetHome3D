const { execSync } = require("child_process");

try {
  // Create a temporary container to copy files into
  console.log("Creating a temporary container...");
  const containerId = execSync("docker create sweethome3d-build")
    .toString()
    .trim();

  // Copy the local project files to the container
  console.log("Copying project files to the container...");
  execSync(`docker cp . ${containerId}:/workspace`);

  // Start the container and wait for it to complete the build
  console.log("Starting the container...");
  execSync(`docker start -ai ${containerId}`);

  // Copy build artifacts from the container
  console.log("Copying build artifacts...");
  execSync(`docker cp ${containerId}:/workspace/install ./build`);

  // Remove the temporary container
  console.log("Removing the container...");
  execSync(`docker rm -f ${containerId}`);

  console.log("Build completed successfully. Artifacts are in the ./build directory.");
} catch (err) {
  console.error("An error occurred:", err.message);
  process.exit(1);
}
