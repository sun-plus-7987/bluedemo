package com.neusof.zengxu.bluemusic;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    Button btopen,btclose,btfind;
    RecyclerView recycleListView;
    List<BluetoothDevice> mbluetoothDeviceList;
    BluetoothListAdpater mbluetoothListAdpater;
    Object mA2dp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mbluetoothDeviceList = new ArrayList<>();
        btopen = findViewById(R.id.btopen);
        btclose = findViewById(R.id.btclose);
        btfind = findViewById(R.id.btfind);
        recycleListView = findViewById(R.id.RecyclerView);
        //初始化列表
        showList();

        //获取BluetoothAdapter对象
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.getProfileProxy(this, mListener, BluetoothProfile.A2DP);

        btopen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断设备是否支持蓝牙，如果mBluetoothAdapter为空则不支持，否则支持
                if (mBluetoothAdapter == null) {
                    Toast.makeText(MainActivity.this, "这台设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                } else {
                    // If BT is not on, request that it be enabled.
                    // setupChat() will then be called during onActivityResult
                    //判断蓝牙是否开启，如果蓝牙没有打开则打开蓝牙
                    if (!mBluetoothAdapter.isEnabled()) {
                        //请求用户开启
                        Intent enableIntent = new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, 1);
                    } else {
                        //getDeviceList();
                    }
                }
            }
        });

        btclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        btfind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mbluetoothDeviceList.clear();
                doDiscovery();
            }
        });
    }

    private void showList() {
        mbluetoothListAdpater = new BluetoothListAdpater(this,mbluetoothDeviceList);
        recycleListView.setLayoutManager(new GridLayoutManager(MainActivity.this,2));
        //添加Android自带的分割线
        recycleListView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recycleListView.setLayoutManager(layoutManager);
        recycleListView.setAdapter(mbluetoothListAdpater);
    }

    public void upData(List<BluetoothDevice> data)
    {
        mbluetoothListAdpater = new BluetoothListAdpater(this, data);
        recycleListView.setAdapter(mbluetoothListAdpater);
        initListener();
    }

    private void initListener() {
        mbluetoothListAdpater.setOnItemClickListener(new BluetoothListAdpater.OnItemClickListener() {
            @Override
            public void onItemClick(BluetoothDevice bluetoothDevice) {
                //在配对之前，停止搜索
                mBluetoothAdapter.cancelDiscovery();
                //获取要匹配的BluetoothDevice对象，后边的deviceList是你本地存的所有对象
                BluetoothDevice device = bluetoothDevice;
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {//没配对才配对
                    try {
                        Log.d("TAG", "开始配对...");
                        Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
                        if (returnValue){
                            Log.d("TAG", "配对成功...");
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mbluetoothListAdpater.setOnItemLongClickListener(new BluetoothListAdpater.OnItemLongClickListener() {
            @Override
            public void OnLongClick(BluetoothDevice bluetoothDevice) {
                Toast.makeText(MainActivity.this, "长按了", Toast.LENGTH_SHORT).show();
                connectA2dp(bluetoothDevice);
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter1 = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED);//搜索开始的过滤器
        IntentFilter filter2 = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//搜索结束的过滤器
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_FOUND);//寻找到设备的过滤器
        IntentFilter filter4 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//绑定状态改变
        IntentFilter filter5 = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);//配对请求

        registerReceiver(mFindBlueToothReceiver ,filter1);
        registerReceiver(mFindBlueToothReceiver ,filter2);
        registerReceiver(mFindBlueToothReceiver ,filter3);
        registerReceiver(mFindBlueToothReceiver ,filter4);
        registerReceiver(mFindBlueToothReceiver ,filter5);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mFindBlueToothReceiver);
    }

    /**
     * 设备是否支持蓝牙  true为支持
     * @return
     */
    public boolean isSupportBlue(){
        return mBluetoothAdapter!= null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                // bluetooth is opened
                //可以获取列表操作等
                Toast.makeText(this, "蓝牙开启", Toast.LENGTH_SHORT).show();
            } else {
                // bluetooth is not open
                Toast.makeText(this, "蓝牙没有开启", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doDiscovery() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.cancelDiscovery();
                }
                mBluetoothAdapter.startDiscovery();
            }
        }).start();
    }

    /*
     *获取已经配对的设备
     */
    private void setPairingDevice() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices.size() > 0) { //存在已配对过的设备
            //利用for循环读取每一个设备的信息
            for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext(); ) {
                BluetoothDevice btd = it.next();
            }
        }else{
            //不存在已经配对的蓝牙设备
        }
    }

    private void connectA2dp(BluetoothDevice device){
        setPriority(device, 100); //设置priority
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
            Method connectMethod =BluetoothA2dp.class.getMethod("connect",
                    BluetoothDevice.class);
            connectMethod.invoke(mA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BluetoothProfile.ServiceListener mListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
            if(profile == BluetoothProfile.A2DP){
                mA2dp = null;
            }
        }
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if(profile == BluetoothProfile.A2DP){
                mA2dp = proxy; //转换
            }
        }
    };


    public void setPriority(BluetoothDevice device, int priority) {
        if (mA2dp == null) return;
        try {//通过反射获取BluetoothA2dp中setPriority方法（hide的），设置优先级
            Method connectMethod =BluetoothA2dp.class.getMethod("setPriority",
                    BluetoothDevice.class,int.class);
            connectMethod.invoke(mA2dp, device, priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPriority(BluetoothDevice device) {
        int priority = 0;
        if (mA2dp == null) return priority;
        try {//通过反射获取BluetoothA2dp中getPriority方法（hide的），获取优先级
            Method connectMethod =BluetoothA2dp.class.getMethod("getPriority",
                    BluetoothDevice.class);
            priority = (Integer) connectMethod.invoke(mA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return priority;
    }



    //广播接收器，当远程蓝牙设备被发现时，回调函数onReceiver()会被执行
    private  BroadcastReceiver mFindBlueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action){
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d("TAG", "开始扫描...");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d("TAG", "结束扫描...");
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    if (device.getName()!=null){
                        mbluetoothDeviceList.add(device);
                        upData(mbluetoothDeviceList);
                        Log.d("TAG", "发现设备..."+device.getName()+"   "+device.getAddress());
                    }
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    Log.d("TAG", "设备绑定状态改变...");
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDING:
                            Log.w("TAG", "正在配对......");
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            Log.w( "TAG", "配对完成");
                            break;
                        case BluetoothDevice.BOND_NONE:
                            Log.w("TAG", "取消配对");
                        default:
                            break;
                    }
                    break;
            }
        }
    };

}
