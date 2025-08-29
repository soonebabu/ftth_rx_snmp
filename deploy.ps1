# -----------------------------
# Deployment Script: Windows -> Ubuntu
# -----------------------------

# --- Configuration ---
$RemoteUser = "barunntc"                                # SSH user
$RemoteHost = "192.168.116.177"                         # Server IP
$RemotePath = "/home/barunntc/snmp-app"                 # Server folder to copy JAR and build Docker image
$JarName = "ftth-monitor-SNMPWalk3-1.SNAPSHOT.jar"     # Local JAR file name
$DockerImage = "ftth-monitor-image-snmpwalk-3"
$DockerRunOptions = "--rm --network host"

# --- 1. Build Maven project locally ---
Write-Host "Building project with Maven..."
mvn clean package

if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed. Exiting."
    exit 1
}

# --- 2. Copy JAR to remote server ---
Write-Host "Copying JAR to remote server..."
scp -C -o ServerAliveInterval=60 ".\target\$JarName" ($RemoteUser + "@" + $RemoteHost + ":" + $RemotePath)



if ($LASTEXITCODE -ne 0) {
    Write-Error "SCP failed. Exiting."
    exit 1
}

# --- 3. SSH to server, build Docker image, and run container ---
Write-Host "Deploying on server..."
ssh $RemoteUser@$RemoteHost @"
echo 'Building Docker image...'
docker build -t $DockerImage $RemotePath

echo 'Running Docker container...'
docker run $DockerRunOptions $DockerImage
"@

Write-Host "Deployment complete!"
