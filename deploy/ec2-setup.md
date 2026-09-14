# Deploying to one AWS EC2 instance

This runbook deploys exactly the same application that was tested locally.
Nothing about the code changes - only the host and the network boundary.

Everywhere you see `<...>`, replace it with the value your instructor
approved for this course, or your own choice if none was specified. None of
these values (account, region, AMI, instance size, IP addresses) should ever
be committed to the repository.

## 0. Before you start

Confirm with your instructor (or your course material) if not already fixed:

- AWS account / region to use.
- Approved Linux AMI. This runbook assumes **Amazon Linux 2023**, which ships
  Corretto (Amazon's OpenJDK build) via `dnf`. If you're told to use Ubuntu
  instead, swap the package-manager commands in step 3 for `apt`.
- Approved instance type. `t2.micro` or `t3.micro` are typically free-tier
  eligible; use whatever your course/account allows.
- Approved connection method: SSH, EC2 Instance Connect, or Session Manager.
  This runbook uses SSH since it's the most universally scriptable, but the
  install/start/verify commands from step 3 onward are identical regardless
  of how you connect.

## 1. Build the artifact locally

```bash
mvn -q package
ls target/httpserver.jar
```

Only `target/httpserver.jar` needs to be transferred: the HTML/JS/PNG/JPEG
resources are bundled inside it (served from the classpath), so there is a
single deployable artifact - no separate "public resources" folder to keep
in sync on the server.

## 2. Launch the EC2 instance

1. EC2 console -> Launch instance.
2. Name: something descriptive, e.g. `networking-lab-httpserver`.
3. AMI: Amazon Linux 2023 (or your course's approved image).
4. Instance type: `t2.micro` / `t3.micro` (or your course's approved size).
5. Network: default VPC / public subnet, unless told otherwise.
6. Key pair: create or reuse one for SSH access. **Never commit the
   downloaded `.pem` file** - it's already covered by `.gitignore`.
7. Security group - create one for this lab with:
   - SSH (port 22) from **My IP** only.
   - Custom TCP, port `8080` (or whatever `PORT` you choose), from **Anywhere**
     for a short classroom test, or from the range your instructor gives you.
8. Launch, wait until the instance state is `running` and status checks pass.

## 3. Connect and install Java

```bash
ssh -i <path-to-your-key.pem> ec2-user@<instance-public-ip>
```

On the instance (Amazon Linux 2023):

```bash
sudo dnf install -y java-21-amazon-corretto
java -version   # confirm it reports 21.x
```

## 4. Transfer the artifact

From your local machine (not on the instance):

```bash
scp -i <path-to-your-key.pem> target/httpserver.jar ec2-user@<instance-public-ip>:/tmp/httpserver.jar
scp -i <path-to-your-key.pem> deploy/httpserver.service ec2-user@<instance-public-ip>:/tmp/httpserver.service
scp -i <path-to-your-key.pem> deploy/run.sh ec2-user@<instance-public-ip>:/tmp/run.sh
```

Back on the instance:

```bash
sudo mkdir -p /opt/httpserver
sudo mv /tmp/httpserver.jar /opt/httpserver/httpserver.jar
sudo mv /tmp/run.sh /opt/httpserver/run.sh
sudo chmod +x /opt/httpserver/run.sh
sudo chown -R ec2-user:ec2-user /opt/httpserver
```

## 5. Quick manual check (before installing the service)

```bash
PORT=8080 /opt/httpserver/run.sh &
curl -s http://localhost:8080/health
# expect: {"status":"UP"}
kill %1
```

This confirms the jar runs on the instance before wiring it into systemd.

## 6. Install as a systemd service (keeps running after logout)

```bash
sudo mv /tmp/httpserver.service /etc/systemd/system/httpserver.service
sudo systemctl daemon-reload
sudo systemctl enable httpserver
sudo systemctl start httpserver
sudo systemctl status httpserver
```

`Environment=PORT=8080` is set inside the unit file - edit
`/etc/systemd/system/httpserver.service` (and re-run `daemon-reload` +
`restart`) if you need a different port.

## 7. Verify

From inside the instance:

```bash
curl -s http://localhost:8080/health
curl -s "http://localhost:8080/greeting?name=EC2"
```

From your own computer (the actual acceptance evidence for section 7.3.6):

```
http://<instance-public-ip>:8080/
```

The page should load its logo, banner, and JS from EC2, and all four
services (plus the /slow demo) should work through the public address.

## 8. Logs

```bash
journalctl -u httpserver -f       # follow live logs
journalctl -u httpserver -n 100   # last 100 lines
```

## 9. Stop / restart

```bash
sudo systemctl stop httpserver
sudo systemctl restart httpserver
sudo systemctl disable httpserver   # stop it from starting on future boots
```

## 10. Mandatory cleanup (do this before ending the session)

1. `sudo systemctl stop httpserver` and capture whatever logs/screenshots you
   still need.
2. Terminate the EC2 instance from the console; wait until its state shows
   `terminated`.
3. Release any Elastic IP you allocated for this lab, if any.
4. Delete the security group created for this lab once nothing references it.
5. Check the Billing / Cost Explorer view on your account to confirm nothing
   from this lab is still running.
