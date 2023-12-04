import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.eclipse.jgit.api.Git;


public class LightyReleaser {
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java StringReplacer <directory> <current_version> <release_version> <next_dev_phase>");
            System.exit(1);
        }

        String directory = args[0];
        String searchStr = args[1];
        String replaceStr = args[2];
        String nextDevPhase = args[3];

        //Bump docs and scripts
        replaceInFiles(directory, searchStr, replaceStr, ".md");
        replaceInFiles(directory, searchStr, replaceStr, ".sh");
        commitChanges(directory,"Bump docs and scripts to " + replaceStr);

        //Bump versions not managed by maven-release
        replaceInFiles(directory+"/lighty-applications/lighty-rcgnmi-app-aggregator/lighty-rcgnmi-app-docker",
            searchStr, replaceStr, ".xml");
        replaceInFiles(directory+"/lighty-applications/lighty-rnc-app-aggregator/lighty-rnc-app-docker",
            searchStr, replaceStr, ".xml");
        commitChanges(directory,"Bump versions not managed by maven-release to " + replaceStr);

        //Set scm.tag to version
        replaceInFiles(directory, "<tag>HEAD</tag>", "<tag>" + replaceStr + "</tag>", ".xml");
        commitChanges(directory,"Set scm.tag to " + replaceStr);

        //Delete tags for mvn release plugin
        deleteTag(directory, replaceStr, nextDevPhase);

        //Run maven release plugin
        runMavenReleasePrepare(directory, replaceStr, nextDevPhase);

        //Delete tags for mvn release plugin
        deleteTag(directory, replaceStr, nextDevPhase);

        //Bump docs and scripts to next Dev Phase
        replaceInFiles(directory, replaceStr, nextDevPhase, ".md");
        replaceInFiles(directory, replaceStr, nextDevPhase, ".sh");
        commitChanges(directory,"Bump docs and scripts to " + nextDevPhase);

        //Bump versions not managed by maven-release to next Dev Phase
        replaceInFiles(directory+"/lighty-applications/lighty-rcgnmi-app-aggregator/lighty-rcgnmi-app-docker",
            replaceStr, nextDevPhase, ".xml");
        replaceInFiles(directory+"/lighty-applications/lighty-rnc-app-aggregator/lighty-rnc-app-docker",
            replaceStr, nextDevPhase, ".xml");
        commitChanges(directory,"Bump versions not managed by maven-release to " + nextDevPhase);

        //Set scm.tag to HEAD
        replaceInFiles(directory, "<tag>" + replaceStr + "</tag>", "<tag>HEAD</tag>", ".xml");
        commitChanges(directory,"Set scm.tag to HEAD");
    }

    private static void replaceInFiles(String directory, String searchStr, String replaceStr, String fileExtension) {
        try {
            Files.walk(Paths.get(directory))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(fileExtension))
                .forEach(file -> replaceInFile(file.toFile(), searchStr, replaceStr));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void replaceInFile(File file, String searchStr, String replaceStr) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            String modifiedContent = content.replace(searchStr, replaceStr);
            Files.write(file.toPath(), modifiedContent.getBytes());

            System.out.println("Replaced in file: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to replace in file: " + file.getAbsolutePath());
        }
    }

    private static void commitChanges(String directory, String commitMsg) {
        try {
            Git git = Git.open(new File(directory));

            git.add().addFilepattern(".").call();
            System.out.println("Committing :" + commitMsg);
            git.commit().setMessage(commitMsg).call();

            System.out.println("Changes committed.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to commit changes.");
        }
    }

    private static void deleteTag(String directory,String release, String nextDev) {
        try {
            Git git = Git.open(new File(directory));

            // Delete existing tags
            git.tagDelete().setTags(release, nextDev).call();

            System.out.println("Tags deleted: " + String.join(", ", release, nextDev));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("An unexpected error occurred.");
        }
    }
    private static void runMavenReleasePrepare(String directory, String releaseVersion, String nextDevVersion) {

        runMavenReleaseClean(directory);
        try {
            // Construct the release:prepare command with echo to provide input non-interactively
            String command = "echo " + releaseVersion +
                " | mvn release:prepare -Darguments=\"-DskipTests\" -DdevelopmentVersion=" + nextDevVersion;

            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.directory(new File(directory));

            processBuilder.redirectErrorStream(true);  // Redirect error stream to output stream

            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Maven release:prepare completed successfully.");
            } else {
                System.out.println("Maven release:prepare failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed to run Maven release:prepare");
        }
        runMavenReleaseClean(directory);
    }

    private static void runMavenReleaseClean(String directory) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("mvn", "release:clean");
            processBuilder.directory(new File(directory));

            processBuilder.redirectErrorStream(true);  // Redirect error stream to output stream

            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Maven release:clean completed successfully.");
            } else {
                System.out.println("Maven release:clean failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed to run Maven release:prepare.");
        }
    }


}
