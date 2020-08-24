package com.sonydafa.minibus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BusDetailActivity  extends Activity {
    private String nearbyStopName;
    private BusRealTimeStatus busRealTimeStatus;
    private List<String> dataSet;
    private List<Map<String,Object>> adapterData;
    private ListView listView;
    private SimpleAdapter simpleAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int getFlag(Map<String,String>map,String str){
        int flag;
        int my_stop_idx = dataSet.indexOf(nearbyStopName);
        int current_idx =  dataSet.indexOf(str);
        if(current_idx <= my_stop_idx){
            if(map.get(str).equals("将至"))
                flag = 2;
            else if(map.get(str).equals("到达"))
                flag = 1;
            else
                flag = 5;
        }else{
            if(map.get(str).equals("将至"))
                flag = 4;
            else if(map.get(str).equals("到达"))
                flag = 3;
            else
                flag = 5;
        }
        return  flag;
    }
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Map<String,String> map = (Map<String,String>)msg.getData().getSerializable("stopMap");
            if(map==null) return;
            if(dataSet!=null)
                dataSet.clear();
            if(adapterData!=null)
                adapterData.clear();
            dataSet.addAll(map.keySet());
            if(dataSet.size()<2) return;
            ((TextView)findViewById(R.id.bus_direction)).
                    setText(String.format(getResources().getString(R.string.bus_orientation),
                            dataSet.get(0),dataSet.get(dataSet.size()-1)));
            ((TextView)findViewById(R.id.bus_distance_1)).setText(BusRealTimeStatus.getDescribe(map,nearbyStopName));
            ((TextView)findViewById(R.id.bus_distance_2)).setText(BusRealTimeStatus.getDescribeSecond(map,nearbyStopName));
            for(int i=0;i<dataSet.size();i++){
                String str = dataSet.get(i);
                Map<String,Object> m = new HashMap<>();
                String value = str;
                if(!map.get(str).equals(""))
                    value += "("+map.get(str)+")";
                m.put("stopName",value);
                adapterData.add(m);
                ListView listView = findViewById(R.id.bus_detail_listview);
                View item = simpleAdapter.getView(0, null, listView);
                item.measure(0,0);
                int line_len = item.getMeasuredHeight();
                int begin_height = listView.getTop()+line_len*i;
                int flag = getFlag(map,str);
                int my_position_idx = dataSet.indexOf(nearbyStopName);
                int current_idx = dataSet.indexOf(str);
                int color=Color.BLUE;
                if(current_idx<=my_position_idx)
                    color = Color.rgb(255,102,0);
                int begin_Or_end = 0;
                if(i==0) begin_Or_end = 1;
                else if(i==dataSet.size()-1) begin_Or_end = 2;
                MyLine myLine = new MyLine(line_len,begin_height,7,5,getResources(),flag,color,begin_Or_end);
                m.put("picture",myLine);
            }
            simpleAdapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
        }
    };
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_detail_activity);
        Intent intent = getIntent();
        String busKey = intent.getStringExtra("busKey");
        nearbyStopName = intent.getStringExtra("nearbyStopName");
        dataSet = new ArrayList<>();
        busRealTimeStatus = new BusRealTimeStatus(handler);
        if(busKey==null || !busKey.contains("-")) return;
        String []busKeyInfo = busKey.split("-");
        busRealTimeStatus.send(busKeyInfo[0],busKeyInfo[1]);
        listView = (ListView) findViewById(R.id.bus_detail_listview);
        adapterData = new ArrayList<>();
         simpleAdapter = new SimpleAdapter(this,adapterData,R.layout.stop_list_item,
                new String[]{"picture","stopName"},new int[]{R.id.ItemImage,R.id.ItemStopName});
        simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String s) {
                if (view instanceof ImageView && data instanceof Drawable) {
                    ImageView iv = (ImageView) view;
                    iv.setImageDrawable((Drawable) data);
                    return true;
                } else
                    return false;
            }
        });
        View view = View.inflate(this,R.layout.layout_header,null);
        listView.addHeaderView(view);
        listView.setAdapter(simpleAdapter);
        listView.setClickable(false);
        swipeRefreshLayout =findViewById(R.id.swipeLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.blue));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                busRealTimeStatus.send(busKeyInfo[0],busKeyInfo[1]);
                //swipeRefreshLayout.setRefreshing(false);
            }
        });

    }
}
