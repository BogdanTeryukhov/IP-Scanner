import io.javalin.Javalin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.net.util.SubnetUtils;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Setter
@NoArgsConstructor
public class CertificateInfo extends Thread{

    public static final Path pathToCheck = Path.of(".\\toResult.txt");

    public static boolean isAddressValid(String address){
        return address.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+");
    }


    public static void addRoutes(Javalin app) {
        app.get("/IP/{address}/{mask}", ctx -> {
            if (isAddressValid(ctx.pathParam("address") + "/" +  ctx.pathParam("mask"))){
                calculation(ctx.pathParam("address") + "/" +  ctx.pathParam("mask"));
                ctx.result("Successfully processing input addresses: " + ctx.pathParam("address") + "/" +  ctx.pathParam("mask")
                        + ". Check /result to see all domains");
            }else{
                ctx.result("Illegal input address. Please try again");
            }
        });

        app.get("/result", ctx -> {
            ctx.result("Response List: \n" + Files.readString(pathToCheck));
        });
    }

    public static void calculation(String IPWithSubnet) throws IOException {
        SubnetUtils utils = new SubnetUtils(IPWithSubnet);
        String[] allIps = utils.getInfo().getAllAddresses();

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

    public static void main(String[] args){
        Javalin app = Javalin.create().start(8081);
        addRoutes(app);
    }
}
