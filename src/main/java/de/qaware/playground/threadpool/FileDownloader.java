package de.qaware.playground.threadpool;


import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileDownloader {

    private FileDownload downloadFile(String url) throws IOException {
        int bufSize = 1024;
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(5);
        ByteArrayBuffer content = new ByteArrayBuffer(bufSize);
        try {
            BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
            int read;
            byte[] buffer = new byte[bufSize];
            while ((read = bis.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
        } catch (Exception exception) {
            byte[] bytes = ("Offline fallback: " + Thread.currentThread().getName()).getBytes();
            content.append(bytes, 0, bytes.length);
        }

        return new FileDownload(url, content);
    }

    private List<FileDownload> downloadFiles(final List<String> urls) throws InterruptedException, ExecutionException {
        List<FileDownload> downloads = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CompletionService<FileDownload> completionService = new ExecutorCompletionService<>(executor);

        for (String url : urls) {
            Callable<FileDownload> callableTask = () -> downloadFile(url);
            completionService.submit(callableTask);
        }

        Future<FileDownload> downloadFuture;
        while((downloadFuture = completionService.poll(1, TimeUnit.SECONDS)) != null) {
            downloads.add(downloadFuture.get());
        }

        executor.shutdown();

        return downloads;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int filesToDownload = 50;

        List<String> urls = new ArrayList<>();
        for (int i = 0; i < filesToDownload; i++) {
            urls.add("http://api.icndb.com/jokes/random");
        }

        List<FileDownload> fileDownloads = new FileDownloader().downloadFiles(urls);

        for (FileDownload file : fileDownloads) {
            System.out.println(file);
        }
    }

    private class FileDownload {
        private final String url;
        private final ByteArrayBuffer content;

        private FileDownload(String url, ByteArrayBuffer content) {
            this.url = url;
            this.content = content;
        }

        @Override
        public String toString() {
            return "FileDownload{" +
                    "url='" + url + '\'' +
                    ", content=" + new String(content.toByteArray()) +
                    '}';
        }
    }
}
