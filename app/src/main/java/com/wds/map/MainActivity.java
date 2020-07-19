package com.wds.map;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiBoundSearchOption;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MapView mapView;
    private BaiduMap baiduMap;
    private BDLocation mLocation;
    private Button btn_me;
    private Button btn_navigation;
    private WalkNaviLaunchParam mParam;
    private double latitude;
    private TextView mLatitude;
    private TextView mLongitude;
    private double longitude;
    private EditText et_city;
    private EditText et_site;
    private Button btn_site;
    private LatLng lat;
    private PoiSearch mPoiSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //我的位置
        btn_me = (Button) findViewById(R.id.btn_me);
        //经度
        mLongitude = (TextView) findViewById(R.id.longitude);
        //纬度
        mLatitude = (TextView) findViewById(R.id.latitude);
        //导航
        btn_navigation = (Button) findViewById(R.id.btn_navigation);
        //搜索框
        et_city = (EditText) findViewById(R.id.et_city);//城市
        et_site = (EditText) findViewById(R.id.et_site);//地址
        //搜索按钮
        btn_site = (Button) findViewById(R.id.btn_site);
        btn_site.setOnClickListener(this);
        btn_me.setOnClickListener(this);
        btn_navigation.setOnClickListener(this);
        //百度地图
        mapView = (MapView) findViewById(R.id.mapView);
        baiduMap = mapView.getMap();
        //普通地图 ,baiduMap是地图控制器对象
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
       /* //卫星地图
        baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
        //空白地图
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
        //开启交通图
        baiduMap.setTrafficEnabled(true);
        //开启热力图
        baiduMap.setBaiduHeatMapEnabled(true);*/
        //开启地图的定位图层
        baiduMap.setMyLocationEnabled(true);
        initLocation();
        initPermission();
        initMarker();
    }

    private void initMarker() {
        BaiduMap.OnMapClickListener listener=new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                lat = latLng;
                marker(latLng);

            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {

            }
        };
       baiduMap.setOnMapClickListener(listener);
    }

    private void marker(LatLng latLng) {
        //构建marker图标
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon);
        //构建markerOption,用于在地图上添di加marker
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).icon(bitmapDescriptor);
        //在地图上添加marker，并显示
        baiduMap.addOverlay(markerOptions);
    }


    //定位
    //构造地图数据
    //我们通过继承抽象类BDAbstractListener并重写其onReceieveLocation方法来获取定位数据，并将其传给MapView。
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }
            mLocation = location;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            baiduMap.setMyLocationData(locData);
        }
    }

    private void initLocation() {
        //定位初始化
        LocationClient mLocationClient = new LocationClient(this);
        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        //设置locationClientOption
        mLocationClient.setLocOption(option);
        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    //Android 6.0 权限适配
    private void initPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            String[] per = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.VIBRATE,
                    Manifest.permission.INTERNET

            };
            ActivityCompat.requestPermissions(this, per, 100);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_me:
                locationToMe();
                break;
            case R.id.btn_navigation:
                navigation();
                break;
            case R.id.btn_site:
                site();
                break;
        }
    }
    //pio搜索
    private void site() {
        //创建POI检索实例
        mPoiSearch = PoiSearch.newInstance();
        //创建POI检索监听器
        OnGetPoiSearchResultListener listener = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
                if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                    baiduMap.clear();

                    //创建PoiOverlay对象
                    PoiOverlay poiOverlay = new PoiOverlay(baiduMap);

                    //设置Poi检索数据
                    poiOverlay.setData(poiResult);

                    //将poiOverlay添加至地图并缩放至合适级别
                    poiOverlay.addToMap();
                    poiOverlay.zoomToSpan();
                }
            }
            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

            }
            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
            //废弃
            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

            }
        };
        mPoiSearch.setOnGetPoiSearchResultListener(listener);
        String city = et_city.getText().toString();
        String site = et_site.getText().toString();
        mPoiSearch.searchInCity(new PoiCitySearchOption()
                .city(city) //必填
                .keyword(site) //必填
                .pageNum(10));
        //搜索半径100米以内
        mPoiSearch.searchNearby(new PoiNearbySearchOption()
                .location(new LatLng(latitude,longitude))
                .radius(10000)
                .keyword(site) //必填
                .pageNum(10));


    }
    private static final String TAG = "MainActivity";

    //导航
    private void navigation() {
        // 获取导航控制类
        // 引擎初始化
        WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {

            @Override
            public void engineInitSuccess() {
                //引擎初始化成功的回调
                routeWalkPlanWithParam();
                Log.e(TAG, "engineInitSuccess: ");
            }

            @Override
            public void engineInitFail() {
                //引擎初始化失败的回调
                Log.e(TAG, "engineInitFail: ");
            }
        });
    }

    private void routeWalkPlanWithParam() {

        //起终点位置
        LatLng startPt = new LatLng(latitude, longitude);
        LatLng endPt = new LatLng(lat.latitude, lat.longitude);
        //构造WalkNaviLaunchParam
        mParam = new WalkNaviLaunchParam().stPt(startPt).endPt(endPt);
        //发起算路
        WalkNavigateHelper.getInstance().routePlanWithParams(mParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                //开始算路的回调
                Log.e(TAG, "onRoutePlanStart: ");
            }

            @Override
            public void onRoutePlanSuccess() {
                //算路成功
                //跳转至诱导页面
                Intent intent = new Intent(MainActivity.this, WNaviGuideActivity.class);
                startActivity(intent);
                Log.e(TAG, "onRoutePlanSuccess: ");
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {
                //算路失败的回调
                Log.e(TAG, "onRoutePlanFail: ");
            }
        });
    }

    //定位自己当前的位置
    private void locationToMe() {
        //如果已经定位了，只需要将地图界面移动到用户所在位置即可
        //改变地图手势的中心点（地图的中心点）
        //mLocation 是定位时获取到的用户位置信息对象
        latitude = mLocation.getLatitude();
        mLatitude.setText("纬度：" + latitude);
        longitude = mLocation.getLongitude();
        mLongitude.setText("经度：" + longitude);
        LatLng latLng = new LatLng(latitude, longitude);
        //改变地图手势中心点
        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(latLng));

    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mapView.onDestroy();
    }
}