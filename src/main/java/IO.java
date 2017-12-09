import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Andrew Tang
 */
public class IO {
    // Pulled this variable out of writeInputToFile so the timer that displays the loading bar is guaranteed access
    private static int totalRead = 0;

    private static void showFileSize(int contentLength) {
        double sizeInMb = (contentLength / ((double)(1024*1024)));
        double sizeInKb = (contentLength / 1024);

        double size = sizeInMb > 1 ? sizeInMb : sizeInKb;
        String unit = sizeInMb > 1 ? "MB" : "KB";

        Main.print("File size: " + new DecimalFormat("#.##").format(size) + unit);
    }

    public static File getFileFromURL(String filePath, String urlPath, boolean showFileSize) throws IOException {
        URL url = new URL(urlPath);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();;

        File file = new File(filePath);
        int responseCode = httpConnection.getResponseCode();

        // HTTP redirect
        if (responseCode % 300 < 100) {
            String redirect = httpConnection.getHeaderField("Location");
            Main.print("Redirecting to " + redirect);
            return getFileFromURL(filePath, redirect, showFileSize);
        }
        // Everything else
        else if (responseCode != 200) {
            Main.print("HTTP " + responseCode + ": " + httpConnection.getResponseMessage());
            System.exit(-1);
        }

        int contentLength = httpConnection.getContentLength();
        if (showFileSize) {
            showFileSize(contentLength);
        }

        InputStream inputStream = httpConnection.getInputStream();
        writeInputToFile(inputStream, file, contentLength);
        return file;
    }

    private static void updateLoadingBar () {

    }

    public static void writeInputToFile (InputStream inputStream, File outputFile) throws FileNotFoundException{
        writeInputToFile(inputStream, outputFile, 0);
    }

    public static void writeInputToFile(InputStream inputStream, File outputFile, int contentSize)
            throws FileNotFoundException {

        BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutputStream);

        byte[] buffer = new byte[1024];
        int bytesRead;

        Timer timer = new Timer();
        if (contentSize > 0) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    double fraction = totalRead / (double) contentSize;
                    char blockFill = '▉';
                    char blockEmpty = ' ';
                    int total_blocks = 50;
                    double percentageBlocks = Math.ceil(fraction*total_blocks);
                    double leftOver = total_blocks - percentageBlocks;

                    System.out.print("\r|");
                    for (int i = 0; i <= percentageBlocks; i++) {
                        System.out.print(blockFill);
                    }
                    for (int i = 0; i < leftOver; i++) {
                        System.out.print(blockEmpty);
                    }
                    double percentageCompleted = Math.floor(fraction*100*10)/10;
                    System.out.print("| " + percentageCompleted + "% complete");

                }
            }, 0, 100);
        }

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bufferedOutput.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            bufferedInput.close();
            bufferedOutput.close();

            // Let the timer finish writing the loading bar by allowing 1 more tick
            Thread.sleep(200);
            timer.cancel();
            totalRead = 0;
        }
        catch (IOException exception) {
            exception.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
