package com.cloud.duolib.model.util.shell;

public abstract class ITarCommander {

    public ITarCommander(){

    }

    public String  startCommand(String cmd, int timeOutMillSec){
        try {
            String result = new ExeCommand().run(cmd,timeOutMillSec).getResult();
            return result;
        }catch (Exception e){
            e.printStackTrace();
        }

        return "";
    }
}
