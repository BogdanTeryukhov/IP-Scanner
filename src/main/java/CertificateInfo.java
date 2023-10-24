import io.javalin.Javalin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.net.util.SubnetUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
public class CertificateInfo extends Thread{
    private List<String> responseList;

    public static void addRoutes(Javalin app) {
        app.get("/IP/{address}/{mask}", ctx -> {
            calculation(ctx.pathParam("address") + "/" +  ctx.pathParam("mask"));
            ctx.result("Successfully processing input addresses: " + ctx.pathParam("address") + "/" +  ctx.pathParam("mask")
            + ". Check /result to see all domains");
        });

        app.get("/result", ctx -> {
            ctx.result("Response List: \n" + Files.readString(Path.of(".\\toResult.txt")));
        });
    }

    public static void calculation(String IPWithSubnet) throws IOException {
        SubnetUtils utils = new SubnetUtils(IPWithSubnet);
        String[] allIps = utils.getInfo().getAllAddresses();

        Path pathToCheck = Path.of(".\\toResult.txt");
        if (Files.exists(pathToCheck)){
            Files.delete(pathToCheck);
        }

        Thread[] threads = new Thread[allIps.length];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new CalcThread(allIps[i]);
            threads[i].start();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }

        BufferedReader reader = new BufferedReader(new FileReader(pathToCheck.toFile()));
        CertificateInfo cert = new CertificateInfo();

        List<String> proxy = new ArrayList<>();
        String line = reader.readLine();
        while (line != null){
            line = reader.readLine();
            if (line == null){
                break;
            }
            proxy.add(line);
        }
        cert.setResponseList(proxy);

        reader.close();
    }

    public static void main(String[] args){
        Javalin app = Javalin.create().start(8081);
        addRoutes(app);
    }


    @Getter
    @Setter
    static class CalcThread extends Thread{
        private String IP;

        public CalcThread(String IP){
            this.IP = IP;
        }

        @Override
        public void run() {
            try {
                InetAddress inetAddress = InetAddress.getByName(IP);
                certInformation("https://" + inetAddress.getHostName());
            } catch (Exception e) {
                return;
            }
        }

        public static void certInformation(String aURL) throws Exception {
            URL destinationURL = new URL(aURL);
            HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
            try {
                conn.connect();
            } catch (ConnectException e) {
                return;
            }

            if (!(conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                return;
            } else {
                Certificate[] certs = conn.getServerCertificates();

                Path pathToFile = Path.of(".\\temp.txt");

                if (Files.exists(pathToFile)) {
                    Files.delete(pathToFile);
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(pathToFile.toFile(), true));
                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate) {
                        X509Certificate x = (X509Certificate) cert;
                        writer.write(String.valueOf(x));
                    }
                }
                writer.close();

                Path toRes = Path.of(".\\toResult.txt");
                BufferedWriter toResult = new BufferedWriter(new FileWriter(toRes.toFile(), true));
                BufferedReader reader = new BufferedReader(new FileReader(pathToFile.toFile()));

                String line = reader.readLine();
                while (line != null) {
                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.matches(".*DNSName:.*")) {
                        toResult.write(line.trim() + "\n");
                    }
                }
                toResult.close();
                reader.close();
                Files.delete(pathToFile);

                removeRepetitions(toRes);
            }
        }

        public static void removeRepetitions(Path path) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(path.toFile()));
            Set<String> lines = new HashSet<>(10000); // maybe should be bigger
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()));
            for (String unique : lines) {
                writer.write(unique);
                writer.newLine();
            }
            writer.close();
        }
    }
}
