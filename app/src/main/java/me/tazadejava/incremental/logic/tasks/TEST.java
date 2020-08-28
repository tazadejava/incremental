package me.tazadejava.incremental.logic.tasks;

import java.io.File;
import java.net.URI;

import me.tazadejava.incremental.ui.main.Utils;

public class TEST {

    public static void main(String[] args) {
        URI fileURI = URI.create("jar:file:/home/yoshi/test/test.zip");
//        saveZipFile(fileURI, new File("/home/yoshi/test/data/"));

//        Utils.zipFiles(new File("/home/yoshi/test/data/"), new File("/home/yoshi/test/testnew.zip"));
        Utils.unzipFile(new File("/home/yoshi/test/testnew.zip"));
    }

//    private static boolean saveZipFile(URI zipFileLocation, File directory) {
//        if(!directory.isDirectory()) {
//            return false;
//        }
//
//        Map<String, String> env = new HashMap<>();
//        env.put("create", "true");
//
//        try (FileSystem zip = FileSystems.newFileSystem(zipFileLocation, env)) {
//            Files.createDirectories(zip.getPath(directory.getName() + "/"));
//            saveAllFiles(zip, directory.toPath(), directory.getName() + "/");
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }
//
//    private static void saveAllFiles(FileSystem zip, Path path, String mainDirectoryPath) throws IOException {
//        for(File file : path.toFile().listFiles()) {
//            if(file.isDirectory()) {
//                String newDirectoryPath = mainDirectoryPath + file.getName() + "/";
//                Files.createDirectories(zip.getPath(newDirectoryPath));
//                saveAllFiles(zip, file.toPath(), newDirectoryPath);
//            } else {
//                Files.copy(file.toPath(), zip.getPath(mainDirectoryPath + file.getName()), StandardCopyOption.REPLACE_EXISTING);
//            }
//        }
//    }
}
