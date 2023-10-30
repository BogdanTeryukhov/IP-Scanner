import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.net.util.SubnetUtils;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class CertificateInfo extends Thread{

    public static final Path pathToCheck = Path.of(".\\toResult.txt");

    private static final HashMap<String, Integer> ipMap = new HashMap<>();


    public static boolean isAddressValid(String address){
        return address.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,2}/\\d{1,2}");
    }


    public static void addRoutes(Javalin app) {
        app.get("/", ctx -> {
            ctx.redirect("/ip-input.html");
        });

        app.get("/result", ctx -> {
            ctx.result("Response List: \n" + Files.readString(pathToCheck));
        });
        

        app.post("/ip-input", ctx -> {
            ipMap.put(ctx.formParam("ip"), Integer.valueOf(Objects.requireNonNull(ctx.formParam("threads"))));
            if (isAddressValid(Objects.requireNonNull(ctx.formParam("ip")))){
                calculation(ctx.formParam("ip"), Integer.parseInt(Objects.requireNonNull(ctx.formParam("threads"))));
                ctx.result("Successfully processing input addresses: " + ctx.formParam("ip") + ". Check /result to see all domains");
            }else{
                ctx.result("Illegal input address. Please try again");
            }
        });
    }

    public static List<String> helper(int start, int end, String[] allIps){
        List<String> result = new ArrayList<>();
        for (int i = start; i < end; i++) {
            result.add(allIps[i]);
        }
        return result;
    }

    public static void calculation(String IPWithSubnet, int numOfThreads) throws IOException {
        SubnetUtils utils = new SubnetUtils(IPWithSubnet);
        String[] allIps = utils.getInfo().getAllAddresses();

        if (Files.exists(pathToCheck)){
            Files.delete(pathToCheck);
        }

        if (numOfThreads > allIps.length){
            numOfThreads = allIps.length;
        }

        int count = allIps.length / numOfThreads;
        int additional = allIps.length % numOfThreads; //если не делится на numOfThreads, то добавим к первому потоку

        int start = 0;
        Thread[] threads = new Thread[numOfThreads];
        for (int i = 0; i < numOfThreads; i++) {
            int cnt = ((i == 0) ? count + additional : count);
            List<String> proxy = helper(start, start + cnt - 1, allIps);
            threads[i] = new CalcThread(proxy, start, start + cnt - 1);
            start += cnt;
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

    public static void main(String[] args){
        Javalin app = Javalin.create(javalinConfig ->
                javalinConfig.addStaticFiles("/html", Location.CLASSPATH)
        ).start(8081);
        addRoutes(app);
    }
}
