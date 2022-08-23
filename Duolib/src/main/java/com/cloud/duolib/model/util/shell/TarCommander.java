package com.cloud.duolib.model.util.shell;


public class TarCommander extends ITarCommander{

    public String startDeleteFoldCmd(int timeOutMillSec,String mFolderDir) throws Throwable {
        String result = "";
        try {
            String cmdCompress = String.format("rm -r %s", mFolderDir);
            result = startCommand(cmdCompress, timeOutMillSec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public interface OnCommandCallback{
        void commandCallback(String result) throws Throwable;
        void commandVirtualProgress(int progress);
    }
}
