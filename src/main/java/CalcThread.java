import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class CalcThread extends Thread{
    private List<String> IpList;
    private int startIndex;
    private int endIndex;

    @Override
    public void run() {
        try {
            for (int i = 0; i < IpList.size(); i++) {
                InetAddress inetAddress = InetAddress.getByName(IpList.get(i));
                certInformation("https://" + inetAddress.getHostName());
            }
        } catch (Exception e) {
            return;
        }
    }

    public static void certificateScanning(Path pathToFile, Certificate[] certs) throws IOException {
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
    }

    public static void readFile(Path toRes, Path pathToFile) throws IOException {
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
            System.out.println("I am here!!!!");
            Path toRes = Path.of(".\\toResult.txt");
            Path pathToFile = Path.of(".\\temp.txt");
            certificateScanning(pathToFile, certs);
            readFile(toRes, pathToFile);
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