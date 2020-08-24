package com.sonydafa.minibus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
//这个类是多线程方法，不可以用类公共变量进行通信，否则多个线程一起读写，最后必然出错了
public class BusRealTimeStatus {
    private Handler handler;
    private static String failure_tips = "不经本站";
    public BusRealTimeStatus(Handler handler){
        this.handler = handler;
    }
    public static String getNextStopName(Map<String,String>map,String location){
        List<String> list = new ArrayList<>(map.keySet());
        if(list.size()<1) return "";
        int idx = list.indexOf(location);
        if(idx+1 < list.size()){
            return "开往 - "+list.get(idx+1);
        }else{
            return "开往 - "+list.get(list.size()-1);
        }
    }
    public static String getDescribeSecond(Map<String,String>map,String location){
        ArrayList<String> list = new ArrayList<>(map.keySet());
        int my_idx = list.indexOf(location);
        if(my_idx==-1) return failure_tips;
        ArrayList<Integer> diffs = new ArrayList<>();
        for(int i=0;i<my_idx;i++){
            if(!map.get(list.get(i)).equals("")){
                diffs.add(my_idx-i);
            }
        }
        Collections.sort(diffs);
        if(diffs.size()<2) return "未发车";
        int distance = diffs.get(1);
        if(distance==0 && !map.get(location).equals(""))
            return map.get(location);
        else
            return distance+"站";
    }
    public static String getDescribe(Map<String,String>map,String localtion){
        List<String> keySets = new ArrayList<>(map.keySet());
        if(!keySets.contains(localtion))
            return failure_tips;
        int prev=-1;
        int my_pos = 0;
        int i=0;
        for(String key:map.keySet()){
            String str = map.get(key);
            if(str!=null && !str.equals("")){
                prev = i;
            }
            if(key.equals(localtion)){
                my_pos = i;
                break;
            }
            i++;
        }
        if(prev == -1){
            return "未发车";
        }
        int diff = my_pos - prev;
        String current = map.get(localtion);
        if(current!=null && !current.equals(""))
            return  current;
        else
            return diff+"站";
    }
    private String requestHttp(String url_str){
        try{
            URL url = new URL(url_str);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder str = new StringBuilder();
            while((line=reader.readLine())!=null){
                str.append(line);
            }
            connection.disconnect();
            return str.toString();
        }catch (IOException e){
            e.printStackTrace();
            return "";
        }
    }
    //查询公交推荐路线
    public void queryRecommendLine(String key){
        Log.i("Bus","查询的内容是:"+key);
        if(key.equals("")) return;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                String url = "http://androidbus.wuhancloud.cn:9087/app/v5/420100/search?keyword="+key;
                String data =  requestHttp(url);
                Log.i("Bus","查询到的内容:"+data);
                List<Map<String,String>> list = new ArrayList<>();
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("l");
                    //为空
                    if(jsonArray.length()<1)
                        return;
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject tmp = jsonArray.getJSONObject(i);
                        String busKey = tmp.getString("i");
                        String name = tmp.getString("n");
                        String start = tmp.getString("s");
                        String end = tmp.getString("e");
                        Map<String,String> map = new HashMap<>();
                        map.put("busKey",busKey);
                        map.put("name",name);
                        map.put("start",start);
                        map.put("end",end);
                        list.add(map);
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
                Log.i("BusRealTimeStatus",list.toString());
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putInt("status",3);
                bundle.putSerializable("recommendLine", (Serializable) list);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        });
        th.start();
    }
    public void send(String busLine,String direction){
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                //发送网络请求
                String url_1 = "http://bus.wuhancloud.cn:9087/website/web/420100/line/027-"+busLine+"-"+direction+".do";
                String str = requestHttp(url_1);
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putInt("status",1);
                bundle.putString("busKey",busLine+"-"+direction);
                msg.setData(bundle);
                //装载map中
                Map<String,String> map = new LinkedHashMap<>();
                try {
                    //处理json数据
                    JSONObject jsonObject = new JSONObject(str);
                    JSONObject data = jsonObject.getJSONObject("data");
                    String linename = data.getString("lineName");
                    //没有查询到
                    if(linename.equals("")){
                        Log.i("BusRealTimeStatus","没有查询到");
                     //do-nothing
                    }
                    String stratStopName = data.getString("startStopName");
                    String endStopName = data.getString("endStopName");
                    JSONArray jsonArray = data.getJSONArray("stops");
                    JSONArray runBus = data.getJSONArray("buses");


                    for(int i=0;i<jsonArray.length();i++){
                        map.put(jsonArray.getJSONObject(i).getString("stopName"),"");
                    }
                    for(int i=0;i<runBus.length();i++){
                        String tmp = runBus.getString(i);
                        String [] runBuses = tmp.split("\\|");
                        int idx = Integer.parseInt(runBuses[2]);
                        int flag = Integer.parseInt(runBuses[3]);
                        int cnt = 0;
                        idx -= 1;
                        for(String key:map.keySet()){
                            if(cnt == idx){
                                String value = (flag==1)?"到达":"将至";
                                map.put(key,value);
                            }
                            cnt++;
                        }
                    }
                    bundle.putSerializable("stopMap", (Serializable) map);
                } catch (JSONException e){
                    e.printStackTrace();
                    bundle.putSerializable("stopMap",(Serializable) map);
                }finally {
                    handler.sendMessage(msg);
                }
            }
        });
        th.start();
    }
}
