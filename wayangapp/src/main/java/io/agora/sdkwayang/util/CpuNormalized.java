package io.agora.sdkwayang.util;

import android.os.Build;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CpuNormalized {

    final static String TAG = "IOTWY/CpuNorm";
    final static int POLICY_FOREACH = 0;
    final static int POLICY_ALL_IN_ONE = 1;

    final static int DEVICEINFO_UNKNOWN = -1;
    private int cpu_cores ;
    private int cpu_cores_active;
    private List<String> policyList , lastFreqStateList , curFreqStateList ;
    private int policy ;
    private long maxFreq;
    private long maxFreq_active;

    public static int percent = 0;
    public static long curFreq = 0;

    public CpuNormalized() {
        cpu_cores = getNumberOfCPUCores();
        policy = getCpuPolicy();
        policyList = getCpuPolicyList(policy);
        maxFreq = getTotalCpuMaxFreq();
    }

    // clear cache and re-calculate
    public void reset() {
        lastFreqStateList = null;
        curFreqStateList = null;
        getCurrentCpuPercent();
    }

    // first time call will return 0
    public int getCurrentCpuPercent() {
        if (policy == POLICY_ALL_IN_ONE) {
            curFreqStateList = getCpuCurFreqPolicyInfoInOne(policyList);
        } else {
            curFreqStateList = getCpuCurFreqPolicyInfo(policyList);
        }

        if(lastFreqStateList != null) {
            if (policy == POLICY_ALL_IN_ONE) {
                curFreq = calcCpuFreqInOne(lastFreqStateList,curFreqStateList);
                maxFreq = maxFreq_active * cpu_cores;
            } else {
                curFreq = calcCpuFreq(lastFreqStateList,curFreqStateList);
            }
        }

        lastFreqStateList = curFreqStateList;
        percent = (int) (( curFreq * 100 ) / maxFreq) ;
        Log.d("percent_is", String.valueOf(percent));
        return percent;
    }

    static private long parseLongFromString(@NotNull String number) {
        long t = 0;
        try {
            if(!number.equals("N/A") && !number.equals("")) {
                t = (long) Double.parseDouble(number.trim());
            }
        } catch (Exception e) {
            Log.e(TAG,"parseLongFromString fail " + number);
        }
        return t;
    }

    private long getTotalCpuMaxFreq() {
        long max = 0;
        try {
            for (int i=0;i<cpu_cores;i++) {
                String node =  "/sys/devices/system/cpu/cpu"+i+"/cpufreq/cpuinfo_max_freq";
                String freq = readFromFile(node);
                max += parseLongFromString(freq);
            }
        } catch (Exception e) {
            Log.e(TAG,"getTotalCpuMaxFreq exception " + e.toString());
        }

        return max;
    }

    private int getCpuPolicy() {
        File f = new File("/sys/devices/system/cpu/cpufreq/all_time_in_state");
        if(f.exists())
            return POLICY_ALL_IN_ONE;
        else
            return POLICY_FOREACH;
    }

    private List<String> getCpuCurFreqPolicyInfoInOne(@NotNull List<String> plist) {
        List<String> lines = new ArrayList<>();
        if(plist.size() > 0) {
            List<String> emptyList = new ArrayList<>();
            emptyList.add("");
            emptyList.add(null);

            String node = plist.get(0);
            lines = new ArrayList<>(Arrays.asList(readFromFile(node).trim().split("\n")));
            lines.removeAll(emptyList);
            if(lines.size() > 1 && lines.get(0).contains("freq")) {
                lines.remove(0);
            } else {
                Log.e(TAG,"getCpuCurFreqPolicyInfoInOne error " + lines);
            }
        }

        return lines;
    }

    private List<String> getCpuCurFreqPolicyInfo(@NotNull List<String> plist) {
        List<String> freqList = new ArrayList<>();
        for(int i=0;i<plist.size();i++) {
            String r = readFromFile(plist.get(i));
            freqList.add(r);
        }
        return freqList;
    }

    private long calcCpuFreqInOne(@NotNull List<String> lastlist, @NotNull List<String> curlist) {
        int size = lastlist.size() == curlist.size() ? lastlist.size() : 0;
        long freq = 0, maxFreq = 0;

        String line;
        List<String> line_list;
        try {
            for(int i=0;i<size;i++) {
                line = curlist.get(i);
                line.replace("\r","");
                line_list = new ArrayList<>(Arrays.asList(line.split("\t")));
                line_list.remove("");
                if (i == size -1 && parseLongFromString(line_list.get(0)) > 0) {
                    maxFreq = parseLongFromString(line_list.get(0));
                }

                double fe = parseLongFromString(line_list.get(0));
                if(fe > 0) {
                    for(int l = 1;l<line_list.size();l++) {
                        if (parseLongFromString(line_list.get(l)) > 0){
                            freq += fe;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG,"calcCpuFreqInOne exception " + e.toString());
        }

        maxFreq_active = maxFreq;
        return freq;
    }

    private long calcCpuFreq(@NotNull List<String> lastlist, @NotNull List<String> curlist) {
        if (lastlist.size() != curlist.size())
            return DEVICEINFO_UNKNOWN;

        long allCoreFreq = -1;
        int size = lastlist.size();
        try {
            for (int i = 0; i < size; i++) {
                String[] l = lastlist.get(i).split("\n");
                String[] c = curlist.get(i).split("\n");
                List<Long> freq = new ArrayList<>();
                List<Long> timems = new ArrayList<>();
                int freq_size = l.length == c.length ? l.length : 0;

                for (int f = 0; f < freq_size; f++) {
                    String[] lastFreqList = l[f].split(" ");
                    String[] curFreqList = c[f].split(" ");
                    if (lastFreqList.length == 2 && parseLongFromString(lastFreqList[1]) != parseLongFromString(curFreqList[1])) {
                        freq.add(parseLongFromString(lastFreqList[0]));
                        timems.add(parseLongFromString(curFreqList[1]) - parseLongFromString(lastFreqList[1]));
                    }
                }

                if (freq.size() > 0) {
                    double total = 0, total_time = 0;
                    for (int t = 0; t < freq.size(); t++) {
                        total += freq.get(t) * timems.get(t);
                        total_time += timems.get(t);
                    }
                    total /= total_time;
                    allCoreFreq += total;
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG,"calcCpuFreq error " + e.toString());
            allCoreFreq = DEVICEINFO_UNKNOWN;
        }


        return allCoreFreq;
    }

    private List<String> getCpuPolicyList(int policy) {
        List<String> plist = new ArrayList<>();
        if(policy == POLICY_ALL_IN_ONE) {
            plist.add("/sys/devices/system/cpu/cpufreq/all_time_in_state");
        } else {
            for(int i = 0 ;i < cpu_cores; i++) {
                plist.add("/sys/devices/system/cpu/cpu"+i+"/cpufreq/stats/time_in_state");
            }
        }
        return plist;
    }

    public int getCoresFromFileString(String str) {
        if (str == null || !str.matches("0-[\\d]+$")) {
            return DEVICEINFO_UNKNOWN;
        }
        int cores = Integer.valueOf(str.substring(2)) + 1;
        return cores;
    }

    private int getCoresFromFileInfo(@NotNull String fileLocation) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileLocation);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String fileContents = buf.readLine();
            buf.close();
            return getCoresFromFileString(fileContents);
        } catch (IOException e) {
            return DEVICEINFO_UNKNOWN;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }
    }

    public int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;
        }
        int cores;
        try {
            cores = getCoresFromFileInfo("/sys/devices/system/cpu/possible");
            if (cores == DEVICEINFO_UNKNOWN) {
                cores = getCoresFromFileInfo("/sys/devices/system/cpu/present");
            }
            if (cores == DEVICEINFO_UNKNOWN) {
                //cores = getCoresFromCPUFileList();
            }
        } catch (SecurityException e) {
            cores = DEVICEINFO_UNKNOWN;
        } catch (NullPointerException e) {
            cores = DEVICEINFO_UNKNOWN;
        }
        Log.i(TAG, "cores:" + cores);
        return cores;
    }

    public static String readFromFile(@NotNull String fileLocation) {
        InputStream is = null;
        String fileContents = "";
        try {
            String[] cmd = {"cat",fileLocation};
            is = Runtime.getRuntime().exec(cmd).getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            while (line != null) {
                fileContents += line + "\n";
                line = br.readLine();
            }
        } catch (Exception e) {
            Log.d(TAG, "readFromFile exception " + e.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }
        return fileContents;
    }

}