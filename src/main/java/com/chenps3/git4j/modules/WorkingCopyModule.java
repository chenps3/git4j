package com.chenps3.git4j.modules;

import com.chenps3.git4j.domain.DiffData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * repo内部，不在.git里的文件都属于working copy
 *
 * @Author chenguanhong
 * @Date 2023/11/3
 */
public class WorkingCopyModule {

    /**
     * 接收diff对象，把变更应用到working copy
     */
    public static void write(Map<String, DiffData> diff) {
        //应用变更
        for (var e : diff.entrySet()) {
            Path path = FilesModule.workingCopyPath(e.getKey());
            switch (e.getValue().getStatus()) {
                case ADD:
                    var objectHash = e.getValue().getReceiver() != null ? e.getValue().getReceiver() : e.getValue().getGiver();
                    FilesModule.write(path, ObjectsModule.read(objectHash));
                    break;
                case CONFLICT:
                    FilesModule.write(path, _composeConflict(e.getValue().getReceiver(), e.getValue().getGiver()));
                    break;
                case MODIFY:
                    FilesModule.write(path, ObjectsModule.read(e.getValue().getGiver()));
                    break;
                case DELETE:
                    try {
                        Files.delete(path);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                default:
                    break;
            }
        }
        //文件删除后，空目录也删掉
        try (Stream<Path> stream = Files.list(FilesModule.workingCopyPath(null))) {
            stream.filter(i -> !i.getFileName().toString().equals(".gitlet"))
                    .forEach(FilesModule::removeEmptyDirs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 输入相同文件不同版本对应的hash，返回一个String
     * 以特定格式表示文件冲突
     * 和git不同的是，git会逐行比较，且内容只包含冲突部分
     * 这里会把整个文件的内容都包含
     */
    private static String _composeConflict(String receiverFileHash, String giverFileHash) {
        String receiverFileContent = ObjectsModule.read(receiverFileHash);
        String giverFileContent = ObjectsModule.read(giverFileHash);
        return "<<<<<<\n"
                + receiverFileContent
                + "\n======\n"
                + giverFileContent
                + "\n>>>>>>\n";
    }
}
