package io.zeebe.dmn;

import java.util.Scanner;

public class StandaloneDmnApplication
{

    private static final String DEFAULT_REPO_DIR = "repo";
    private static final String DEFAULT_TOPIC = "default-topic";

    public static void main(String[] args)
    {
        // TODO map args to client properties

        final DmnApplication application = new DmnApplication(DEFAULT_REPO_DIR, DEFAULT_TOPIC);
        application.start();

        waitUntilClose();

        application.close();
    }

    private static void waitUntilClose()
    {
        try (Scanner scanner = new Scanner(System.in))
        {
            while (scanner.hasNextLine())
            {
                final String nextLine = scanner.nextLine();
                if (nextLine.contains("close"))
                {
                    return;
                }
            }
        }
    }

}
