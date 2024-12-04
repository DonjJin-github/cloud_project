package org.example;

import java.util.Scanner;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.jcraft.jsch.*;

public class Main {

    static AmazonEC2 ec2;

    private static void init() throws Exception {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (C:\\Users\\dongjin\\.aws\\credentials), and is in valid format.",
                    e);
        }

        // 리전을 ap-southeast-2로 고정
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("ap-southeast-2")
                .build();
    }

    public static void main(String[] args) throws Exception {
        init();

        Scanner menu = new Scanner(System.in);
        Scanner idString = new Scanner(System.in);
        int number = 0;

        while (true) {
            System.out.println("\n------------------------------------------------------------");
            System.out.println("           Amazon AWS Control Panel using SDK               ");
            System.out.println("------------------------------------------------------------");
            System.out.println("  1. list instance                2. available zones        ");
            System.out.println("  3. start instance               4. available regions      ");
            System.out.println("  5. stop instance                6. create instance        ");
            System.out.println("  7. reboot instance              8. list images            ");
            System.out.println("  9. execute condor_status                                   ");
            System.out.println("                                 99. quit                   ");
            System.out.println("------------------------------------------------------------");

            System.out.print("Enter an integer: ");
            if (menu.hasNextInt()) {
                number = menu.nextInt();
            } else {
                System.out.println("Invalid input!");
                break;
            }

            String instanceId = "";
            String amiId = "";

            switch (number) {
                case 1:
                    listInstances();
                    break;
                case 2:
                    availableZones();
                    break;
                case 3:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) startInstance(instanceId);
                    break;
                case 4:
                    availableRegions();
                    break;
                case 5:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) stopInstance(instanceId);
                    break;
                case 6:
                    System.out.print("Enter ami id: ");
                    amiId = idString.nextLine().trim();
                    if (!amiId.isEmpty()) createInstance(amiId);
                    break;
                case 7:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) rebootInstance(instanceId);
                    break;
                case 8:
                    listImages();
                    break;
                case 9:
                    executeCondorStatus();
                    break;
                case 99:
                    System.out.println("Exiting...");
                    menu.close();
                    idString.close();
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    public static void listInstances() {
        System.out.println("Listing instances...");
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        boolean done = false;

        while (!done) {
            DescribeInstancesResult response = ec2.describeInstances(request);

            for (Reservation reservation : response.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    System.out.printf(
                            "[id] %s, [AMI] %s, [type] %s, [state] %10s, [monitoring state] %s\n",
                            instance.getInstanceId(),
                            instance.getImageId(),
                            instance.getInstanceType(),
                            instance.getState().getName(),
                            instance.getMonitoring().getState());
                }
            }

            request.setNextToken(response.getNextToken());

            if (response.getNextToken() == null) {
                done = true;
            }
        }
    }

    public static void availableZones() {
        System.out.println("Available zones...");
        DescribeAvailabilityZonesResult result = ec2.describeAvailabilityZones();

        for (AvailabilityZone zone : result.getAvailabilityZones()) {
            System.out.printf("[Zone] %s, [Region] %s, [Zone Name] %s\n",
                    zone.getZoneId(), zone.getRegionName(), zone.getZoneName());
        }
    }

    public static void startInstance(String instanceId) {
        System.out.printf("Starting instance: %s\n", instanceId);

        try {
            StartInstancesRequest request = new StartInstancesRequest()
                    .withInstanceIds(instanceId);
            ec2.startInstances(request);
            System.out.printf("Successfully started instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to start instance: %s\n", e.getMessage());
        }
    }

    public static void stopInstance(String instanceId) {
        System.out.printf("Stopping instance: %s\n", instanceId);

        try {
            StopInstancesRequest request = new StopInstancesRequest()
                    .withInstanceIds(instanceId);
            ec2.stopInstances(request);
            System.out.printf("Successfully stopped instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to stop instance: %s\n", e.getMessage());
        }
    }

    public static void createInstance(String amiId) {
        System.out.printf("Creating instance in region: ap-southeast-2 with AMI: %s\n", amiId);

        try {
            RunInstancesRequest request = new RunInstancesRequest()
                    .withImageId(amiId)
                    .withInstanceType(InstanceType.T2Micro)
                    .withMinCount(1)
                    .withMaxCount(1);
            RunInstancesResult response = ec2.runInstances(request);
            String instanceId = response.getReservation().getInstances().get(0).getInstanceId();
            System.out.printf("Successfully created instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to create instance: %s\n", e.getMessage());
        }
    }

    public static void rebootInstance(String instanceId) {
        System.out.printf("Rebooting instance: %s\n", instanceId);

        try {
            RebootInstancesRequest request = new RebootInstancesRequest()
                    .withInstanceIds(instanceId);
            ec2.rebootInstances(request);
            System.out.printf("Successfully rebooted instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to reboot instance: %s\n", e.getMessage());
        }
    }

    public static void availableRegions() {
        System.out.println("Available regions...");
        DescribeRegionsResult result = ec2.describeRegions();

        for (Region region : result.getRegions()) {
            System.out.printf("[Region] %s, [Endpoint] %s\n",
                    region.getRegionName(), region.getEndpoint());
        }
    }

    public static void listImages() {
        System.out.println("Listing images....");

        DescribeImagesRequest request = new DescribeImagesRequest();
        request.withFilters(new Filter().withName("name").withValues("aws-htcondor-slave"));

        try {
            DescribeImagesResult results = ec2.describeImages(request);

            for (Image image : results.getImages()) {
                System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
                        image.getImageId(), image.getName(), image.getOwnerId());
            }
        } catch (Exception e) {
            System.out.printf("Failed to list images: %s\n", e.getMessage());
        }
    }

    public static void executeCondorStatus() {
        // 고정된 EC2 SSH 정보
        String user = "ec2-user";
        String host = "ec2-52-64-104-139.ap-southeast-2.compute.amazonaws.com";
        String privateKeyPath = "C:\\Users\\dongjin\\cloudtest.pem"; // EC2 키 페어 파일 경로 (사용자 환경에 맞게 수정)

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(privateKeyPath);

            // 세션 생성
            Session session = jsch.getSession(user, host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // 명령 실행
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("condor_status");

            channel.setInputStream(null);
            channel.setErrStream(System.err);

            // 결과 출력
            Scanner outputScanner = new Scanner(channel.getInputStream());
            channel.connect();

            System.out.println("\n--- condor_status output ---");
            while (outputScanner.hasNextLine()) {
                System.out.println(outputScanner.nextLine());
            }
            System.out.println("--- End of output ---");

            outputScanner.close();
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            System.out.printf("Failed to execute condor_status: %s\n", e.getMessage());
        }
    }


}