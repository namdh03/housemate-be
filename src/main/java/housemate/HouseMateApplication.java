package housemate;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * @author ThanhF
 */
@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "HouseMate API",
                version = "1.0.0",
                description = "An API for the software system provides single service and package service for student apartments."
        ),
        servers = {
            @Server(url = "http://localhost:8080"),
            @Server(url = "http://localhost:8888"),
            @Server(url = "https://housemateb.thanhf.dev"),
            @Server(url = "https://housemateb2.thanhf.dev"),
            @Server(url = "https://housemateb3.thanhf.dev"),
            @Server(url = "https://housemate-api.namdh03.site"),
        }
)
public class HouseMateApplication {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(HouseMateApplication.class, args);
    }

}
