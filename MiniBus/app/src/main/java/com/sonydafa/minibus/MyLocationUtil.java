package com.sonydafa.minibus;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
;

public class MyLocationUtil {
    private  Map<String,List<String>> map;
    private Handler handler = null;
    public Map<String,List<String>> getMap() {
        return map;
    }
    public MyLocationUtil(Handler handler){
        this.handler = handler;
    }
    private class MyLocationListener extends BDAbstractLocationListener
    {
        @Override
        public void onReceiveLocation(BDLocation location){
            if(location==null)
                return;
            String locationDescribe = location.getLocationDescribe();    //获取位置描述信息
            Log.i("sonydf",locationDescribe);
            msearch.searchNearby(new PoiNearbySearchOption().keyword("公交站").
                    location(new LatLng(location.getLatitude(),location.getLongitude()))
                    .radius(500));
        }
    }
    private class MyPoiResult implements OnGetPoiSearchResultListener
    {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if(poiResult==null) return;
            List<PoiInfo> allPoi = poiResult.getAllPoi();
            map.clear();
            if(allPoi==null) {
                return;
            }
            for(PoiInfo info:allPoi){
                //double dis = DistanceUtil.getDistance(info.getLocation(),new LatLng(myLocation.getLatitude(),myLocation.getLongitude()));
                String name = info.getName();
                String address = info.getAddress();
                String[] lines = address.split(";");
                List<String> line = new ArrayList<>();
                for(String s:lines){
                    if(s.contains("有轨电车")) continue;
                    int idx = s.indexOf('路');
                    if(idx == -1){
                        Log.i("error",s);
                        continue;
                    }
                    line.add(s.substring(0,idx));
                }
                if(line.size()>0)
                    map.put(name,line);
            }
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putInt("status",2);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {}
        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {}
        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {}
    }

    private PoiSearch msearch;
    public LocationClient mLocationClient = null;

    void start(Context context){
        SDKInitializer.initialize(context);
        //baiduMap
        mLocationClient = new LocationClient(context);
        //声明LocationClient类
        mLocationClient.registerLocationListener(new MyLocationListener());
        //注册监听函数
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedLocationDescribe(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setIsNeedAddress(true);
        option.setIsNeedLocationPoiList(true);
        //option.setScanSpan(1000);
        option.setCoorType("bd09ll");
        option.setOpenGps(true);
        mLocationClient.setLocOption(option);
        //poi init
        msearch = PoiSearch.newInstance();
        msearch.setOnGetPoiSearchResultListener(new MyPoiResult());
        map =new LinkedHashMap<>();
        mLocationClient.start();
    }
}
