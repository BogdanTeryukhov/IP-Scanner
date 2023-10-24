import lombok.Getter;
import lombok.Setter;
import org.apache.commons.net.util.SubnetUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class CertificateInfo extends Thread{

    public static void main(String[] args) throws Exception {
        SubnetUtils utils = new SubnetUtils("140.82.121.0/24");
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

        public static void certInformation(String aURL) throws Exception{
            URL destinationURL = new URL(aURL);
            HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
            try {
                conn.connect();
            }catch (ConnectException e){
                return;
            }

            if (!(conn.getResponseCode() == HttpURLConnection.HTTP_OK)){
                return;
            }else {
                Certificate[] certs = conn.getServerCertificates();

                Path pathToFile = Path.of(".\\temp.txt");

                if (Files.exists(pathToFile)){
                    Files.delete(pathToFile);
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(pathToFile.toFile(),true));
                for (Certificate cert : certs) {
                    //System.out.println("Certificate is: " + cert);
                    if(cert instanceof X509Certificate) {
                        X509Certificate x = (X509Certificate) cert;
                        writer.write(String.valueOf(x));
                    }
                }
                writer.close();

                Path toRes = Path.of(".\\toResult.txt");
                BufferedWriter toResult = new BufferedWriter(new FileWriter(toRes.toFile(), true));
                BufferedReader reader = new BufferedReader(new FileReader(pathToFile.toFile()));

                String line = reader.readLine();
                while (line != null){
                    line = reader.readLine();
                    if (line == null){
                        break;
                    }
                    if (line.matches(".*DNSName:.*")){
                        toResult.write(line.trim() + "\n");
                    }
                }
                toResult.close();
                reader.close();
                Files.delete(pathToFile);
            }
        }
    }
}
