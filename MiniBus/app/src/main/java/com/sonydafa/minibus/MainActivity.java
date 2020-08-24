package com.sonydafa.minibus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.PermissionChecker;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private BusRealTimeStatus busRealTimeStatus = null;
    private MyLocationUtil myLocationUtil = null;
    private List<Map<String,Object>> adapterListData;
    private SimpleAdapter simpleAdapter;
    private  ListView listView;
    private ListView recommentLineListView;
    private ArrayAdapter<String> recommendAdapter;
    private List<String> recommendData;
    private String nearbyStopName = "";
    private SwipeRefreshLayout swipeRefreshLayout;
    //当listview绘制成功，进行绑定click事件
    private final ViewTreeObserver.OnPreDrawListener OnPreDrawListener = () -> {
        bindClick();
        return true;
    };
    private final ViewTreeObserver.OnPreDrawListener OnPreDrawListener2 = () -> {
        bindClick2();
        return true;
    };
    List<Map<String,String>> recommendBusListMap;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            int status = msg.getData().getInt("status");
            //判断一下
            switch (status){
                case 1:
                    String busKey = msg.getData().getString("busKey");
                    Map<String,String> tmpMap = (Map<String,String>)msg.getData().getSerializable("stopMap");
                    Log.i("MainActivity","查询到停的站:"+tmpMap.toString());

                    if(tmpMap.size()<1 || busKey ==null||busKey.equals("")) {
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    String [] buskeyArray = busKey.split("-");
                    String busline = buskeyArray[0];
                    String direction = buskeyArray[1];
                    //查找list中是否包含，包含则删除
                    Map<String,Object> tmp = new HashMap<>();
                    for(int i=0;i<adapterListData.size();i++){
                        String line = (String)adapterListData.get(i).get("id");
                        if(line!=null && line.equals(busline)){
                            tmp = adapterListData.get(i);
                            adapterListData.remove(i);
                            break;
                        }
                    }
                    //更新一条数据
                    tmp.put("id",busline);
                    String nextStopName = BusRealTimeStatus.getNextStopName(tmpMap,nearbyStopName);
                    String describe = BusRealTimeStatus.getDescribe(tmpMap,nearbyStopName);
                    if(direction.equals("0")) {
                        tmp.put("upStop",nextStopName);
                        tmp.put("upDescribe",describe);
                    }else{
                        tmp.put("downStop",nextStopName);
                        tmp.put("downDescribe",describe);
                    }
                    adapterListData.add(tmp);
                    Collections.sort(adapterListData, (stringObjectMap, t1) -> {
                        String str1 = (String)stringObjectMap.get("id");
                        String str2 = (String) t1.get("id");
                        return str1.compareTo(str2);
                    });

                    simpleAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case 2:
                    //清空数据并通知，防止数组访问越界
                    adapterListData.clear();
                    simpleAdapter.notifyDataSetChanged();
                    //清空旧的数据
                    Log.i("sonydf","重新刷新");
                    Map<String,List<String>> map = myLocationUtil.getMap();
                    Log.i("MainActivity",map.toString());
                    if(map.size()<1) {
                        Toast.makeText(getApplicationContext(), "没有查询到公交信息", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    for(String key : map.keySet()){
                        TextView textView = findViewById(R.id.textView);
                        String positionName = getResources().getString(R.string.my_position_name);
                        positionName = String.format(positionName,key);
                        textView.setText(positionName);
                        nearbyStopName = key;
                        List<String> list = map.get(key);
                        if(list==null) break;
                        for(String s:list) {
                            busRealTimeStatus.send(s,"0");
                            busRealTimeStatus.send(s, "1");
                        }
                        break;
                    }
                    break;
                case 3:
                    recommendBusListMap = (List<Map<String,String>>)msg.getData().getSerializable("recommendLine");
                    if(recommendBusListMap==null) return;
                    recommendData.clear();
                    recommendAdapter.notifyDataSetChanged();
                    for(Map<String,String> one:recommendBusListMap){
                        String name = one.get("name");
                        String end = one.get("end");
                        recommendData.add(name+" 开往 "+end);
                    }
                    Log.i("search",recommendData.toString());
                    recommendAdapter.notifyDataSetChanged();
            }
        }
    };
    private void bindClick2(){
        for(int i=0;i<recommentLineListView.getChildCount();i++){
            View vw = recommentLineListView.getChildAt(i);
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,BusDetailActivity.class);
            intent.putExtra("nearbyStopName",nearbyStopName);
            String key;
            if(recommendBusListMap==null || i >= recommendBusListMap.size()) return;
            Map<String,String> map = recommendBusListMap.get(i);
            String busKey = map.get("busKey");
            if(busKey==null) return;
            String []spices = busKey.split("-");
            String trueBusKey="";
            if(spices.length == 3){
                trueBusKey = String.format("%s-%s",spices[1],spices[2]);
            }
            intent.putExtra("busKey", trueBusKey);
            vw.setOnClickListener(view -> {
                startActivity(intent);
            });
        }
    }
    private void bindClick(){
        if(listView==null||listView.getChildCount()<1)
            return;
        //listView[0]是一个分割线, 不需要
        for(int i=1;i<listView.getChildCount();i++){
            LinearLayout layout =(LinearLayout) listView.getChildAt(i);
            TextView tv= (TextView) layout.getChildAt(0);
            LinearLayout linearLayout = (LinearLayout) layout.getChildAt(1);
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,BusDetailActivity.class);
            intent.putExtra("nearbyStopName",nearbyStopName);
            linearLayout.setOnClickListener(view -> {
                intent.putExtra("busKey",tv.getText()+"-0");
                startActivity(intent);
            });
            LinearLayout linearLayout1 = (LinearLayout) layout.getChildAt(2);
            linearLayout1.setOnClickListener(view -> {
                intent.putExtra("busKey",tv.getText()+"-1");
                startActivity(intent);
            });
        }
    }
    private void initAlterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 设置提示信息
        builder.setMessage("应用需要打开位置");
        // 设置按钮
        builder.setPositiveButton("开启", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("start","打开GPS");
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("start","用户未授权");

            }
        });
        // 显示对话框（弹出）
        builder.show();
    }
    public void checkPermission(){
        XXPermissions.with(this).permission(Permission.ACCESS_FINE_LOCATION)
                .permission(Permission.ACCESS_COARSE_LOCATION).permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean all) {
                        if(all){
                            Log.i("permission","已经授予权限");
                        }else{
                            Log.i("permission","部分授予");
                        }
                    }
                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        if (quick) {
                            Toast.makeText(getApplicationContext(),"被永久拒绝授权，请在应用设置中手动授予位置权限",Toast.LENGTH_LONG).show();
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            //XXPermissions.startPermissionActivity(MainActivity.this, denied);
                        } else {
                            Toast.makeText(getApplicationContext(),"拒绝给予",Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    //检测GPS是否打开
    public static Boolean isLocationEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            // This is Deprecated in API 28
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return  (mode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("MainActivity","app start");

        //检查权限
        checkPermission();
        //检查是否打开开关了
        boolean locationEnabled = isLocationEnabled(getApplicationContext());
        if(!locationEnabled){
            //提示用话打开
            Toast.makeText(getApplicationContext(),"请打开GPS开关以便定位",Toast.LENGTH_SHORT).show();
            //initAlterDialog();
        }
        busRealTimeStatus = new BusRealTimeStatus(handler);
        //init listView
        listView = findViewById(R.id.buslist);
        adapterListData = new ArrayList<>();
        simpleAdapter = new SimpleAdapter(this,adapterListData,R.layout.bus_list_item,
                new String[]{"id","upStop","upDescribe","downStop","downDescribe"},
                new int[]{R.id.no,R.id.upStop,R.id.upDescribe,R.id.downStop,R.id.downDescribe});
        View header = View.inflate(this,R.layout.main_header,null);
        listView.addHeaderView(header);
        listView.setAdapter(simpleAdapter);
        listView.getViewTreeObserver().addOnPreDrawListener(OnPreDrawListener);

        //baidu-Map Init
        myLocationUtil = new MyLocationUtil(handler);
        //myLocationUtil.start(getApplicationContext());
        swipeRefreshLayout = findViewById(R.id.swipeLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.blue),getResources().getColor(R.color.colorPrimary));
        SwipeRefreshLayout.OnRefreshListener listener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 初始化定时器
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        if(swipeRefreshLayout.isRefreshing()){
                            Looper.prepare();
                            Toast.makeText(getApplicationContext(),"未获取到信息",Toast.LENGTH_LONG)
                                    .show();
                            swipeRefreshLayout.setRefreshing(false);
                            Looper.loop();
                        }
                        timer.cancel();
                    }
                },3000);
                myLocationUtil.start(getApplicationContext());
            }
        };
        swipeRefreshLayout.setOnRefreshListener(listener);
        swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
        listener.onRefresh();
        //搜索视图
        findViewById(R.id.secondLayout).setVisibility(View.INVISIBLE);
        recommentLineListView = findViewById(R.id.search_recommend_list);
        recommendData = new ArrayList<>();
        recommendAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,recommendData);
        recommentLineListView.setAdapter(recommendAdapter);
        recommentLineListView.getViewTreeObserver().addOnPreDrawListener(OnPreDrawListener2);
    }
    private SearchView searchView;
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setQueryHint("输入公交线路");
        searchView.setOnSearchClickListener(v -> {
            //search is expanded
            Log.i("search","展开");
            if(recommendData!=null)
                recommendData.clear();

            if(recommendBusListMap!=null)
                recommendBusListMap.clear();

            findViewById(R.id.firstLayout).setVisibility(View.INVISIBLE);
            findViewById(R.id.secondLayout).setVisibility(View.VISIBLE);
        });
        searchView.setOnCloseListener(() -> {
            Log.i("search","关闭搜索");
            findViewById(R.id.firstLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.secondLayout).setVisibility(View.INVISIBLE);
            return false;
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                if(s.equals(""))
                    return false;
                busRealTimeStatus.queryRecommendLine(s);
                return false;
            }
        });
        return true;
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //展开
            if(!searchView.isIconified()){
                Log.i("search","关闭searchView");
                searchView.setIconified(true);
                searchView.onActionViewCollapsed();
                findViewById(R.id.firstLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.secondLayout).setVisibility(View.INVISIBLE);
                return false;
            }
            return super.onKeyDown(keyCode,event);
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
