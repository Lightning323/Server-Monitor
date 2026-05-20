package org.pcMonitor.platform;

import java.util.List;

public class SystemInfo {
    public CpuInfo CpuInfo;
    public MemoryInfo MemoryInfo;
}

class CpuInfo {
    public List<Integer> uiLoad;
    public List<Integer> uiTjMax;
    public int uiCoreCnt;
    public int uiCPUCnt;
    public List<Float> fTemp;
    public float fVID;
    public float fCPUSpeed;
    public float fFSBSpeed;
    public float fMultipier;
    public String CPUName;
    public int ucFahrenheit;
    public int ucDeltaToTjMax;
}

class MemoryInfo {
    public long TotalPhys;
    public long FreePhys;
    public long TotalPage;
    public long FreePage;
    public long TotalVirtual;
    public long FreeVirtual;
    public long FreeExtendedVirtual;
    public int MemoryLoad;
}
