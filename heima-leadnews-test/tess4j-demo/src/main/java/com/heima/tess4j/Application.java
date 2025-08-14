package com.heima.tess4j;

import net.sourceforge.tess4j.Tesseract;

public class Application {

    /**
     * 识别图片中的文字
     * @param args
     */
    public static void main(String[] args) {

        //创建实例
        Tesseract tesseract = new Tesseract();

        //设置字体库路径
        tesseract.setDatapath("D:\\zkf314\\Desktop\\leadnews-materials\\chi_sim.traineddata");

        //设置语言
        tesseract.setLanguage("chi_sim");

    }

}
