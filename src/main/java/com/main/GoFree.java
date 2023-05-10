package com.main;

import com.utils.TelegramNotificationBot;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取v2ex免费赠送的内容
 *
 * @author BeamStark
 * @date 2023-05-11-00:24
 */
@Slf4j
@Service
@EnableScheduling
public class GoFree {
    private static final String baseUrl = "https://www.v2ex.com/go/free";
    private static final String dataPath = System.getProperty("user.dir") + "/data/data.dat";

    static TelegramNotificationBot telegramNotificationBot = new TelegramNotificationBot();

    /**
     * 定时任务
     * 1个小时执行一次
     * 比较是否有更新
     * 如果有更新tg通知
     */
    @Scheduled(cron = "0 0 * * * ?")
    private static void domain() {
        log.info("比较开始");
        List<Map<String, String>> differentEntries = getDifferentEntries(parseListMap(), getFree());

        List<String> result = differentEntries.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (differentEntries.size() != 0) {
            telegramNotificationBot.sendMessageWithList("白嫖", result, "v2ex");
        }
    }


    /**
     * 定时任务
     * 6小时执行一次
     * 开始时执行一次
     * 更新data.dat
     */
    @PostConstruct
    @Scheduled(cron = "0 0 */6 * * ?")
    private static void saveData() {
        log.info("更新data.dat");
        System.out.println("------------");
        saveListMap(getFree());
    }

    @SneakyThrows
    private static List<Map<String, String>> getFree() {
        List<Map<String, String>> maps = new ArrayList<>();
        Connection con = Jsoup.connect(baseUrl).timeout(50000);
        Document document = con.get();
        Elements topicsNode = document.getElementById("TopicsNode").getElementsByClass(
                "item_title");

        for (Element t : topicsNode) {
            String title = t.text();
            HashMap<String, String> map = new HashMap<>();
            Element first = t.select("span.item_title a").first();
            String url = "https://www.v2ex.com/" + first.attr("href");
            map.put("title", title);
            map.put("url", url);
            maps.add(map);
        }
        return maps;
    }


    @SneakyThrows
    public static void saveListMap(List<Map<String, String>> listMap) {
        FileOutputStream fos = new FileOutputStream(dataPath);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(listMap);
        oos.close();
        fos.close();
    }

    @SneakyThrows
    public static List<Map<String, String>> parseListMap() {
        FileInputStream fis = new FileInputStream(dataPath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        List<Map<String, String>> listMap = (List<Map<String, String>>) ois.readObject();
        ois.close();
        fis.close();
        return listMap;
    }


    private static List<Map<String, String>> getDifferentEntries(List<Map<String, String>> list1,
                                                                 List<Map<String, String>> list2) {
        List<Map<String, String>> diffList = new ArrayList<>();

        for (Map<String, String> map1 : list1) {
            boolean foundMatch = false;

            for (Map<String, String> map2 : list2) {
                if (map1.equals(map2)) {
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                diffList.add(map1);
            }
        }

        for (Map<String, String> map2 : list2) {
            boolean foundMatch = false;

            for (Map<String, String> map1 : list1) {
                if (map2.equals(map1)) {
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                diffList.add(map2);
            }
        }

        return diffList;
    }
}
