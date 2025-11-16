package snvnUserService.icsQuizUserService.service;

import snvnUserService.icsQuizUserService.model.User;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UserCsvGenerator {

    private final Random random = new Random();

    /**
     * Generates n random users and writes them to a CSV file
     * @param count number of random users to generate
     * @param filePath path where CSV will be saved
     */
    public void generateRandomUsersToCsv(int count, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.append("id,user_id,name,code_number,password,created_at,updated_at\n");

            for (int i = 1; i <= count; i++) {
                long userId = random.nextInt(1_000_000);
                String name = "User_" + userId;
                String codeNumber = "C" + (1000 + random.nextInt(9000));
                String password = "PWD" + (100000 + random.nextInt(900000));
                LocalDateTime now = LocalDateTime.now();

                User user = new User(userId, name, codeNumber, password);
                user.setId((long) i);
                user.setCreatedAt(now);
                user.setUpdatedAt(now);

                // Write CSV row
                writer.append(String.format("%d,%d,%s,%s,%s,%s,%s\n",
                        user.getId(),
                        user.getUserId(),
                        user.getName(),
                        user.getCodeNumber(),
                        user.getPassword(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ));
            }

            System.out.println("✅ CSV file generated successfully at: " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Error while writing CSV file: " + e.getMessage());
        }
    }
}
