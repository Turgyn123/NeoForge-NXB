package net.narutoxboruto.client;

public class PlayerData {

    //INFO
    private static int chakra;
    public static int maxChakra;
    private static int shinobi_points;
    private static String clan, affiliation, rank, releaseList;
   // private static boolean chakraControl, narutoRun;
   // //STATS
    private static int taijutsu, ninjutsu, genjutsu, kenjutsu, kinjutsu, medical, senjutsu, shurikenjutsu, speed, summoning;
    //JUTSUS
    private static String fireList, waterList, yangList, yinList, earthList, windList, lightningList;
    private static String release, fireJutsu, waterJutsu, yangJutsu, yinJutsu, earthJutsu, windJutsu, lightningJutsu;
    private static String selectedRelease;
   // private static ItemStack secondOffhandStack = ItemStack.EMPTY;
    
    private static boolean chakraControlActive = false;
    private static boolean kibaActive = false;
    private static boolean lightningChakraModeActive = false;

    public static int getChakra() {
        return chakra;
    }

    public static void setChakra(int chakra) {
        PlayerData.chakra = chakra;
    }
    
    public static boolean isChakraControlActive() {
        return chakraControlActive;
    }
    
    public static void setChakraControlActive(boolean active) {
        chakraControlActive = active;
    }

    public static boolean isKibaActive() {
        return kibaActive;
    }

    public static void setKibaActive(boolean active) {
        kibaActive = active;
    }

    public static boolean isLightningChakraModeActive() {
        return lightningChakraModeActive;
    }

    public static void setLightningChakraModeActive(boolean active) {
        lightningChakraModeActive = active;
    }

    public static int getMaxChakra() {
        return maxChakra;
    }

    public static int setMaxChakra(int maxChakra) {PlayerData.maxChakra = maxChakra; return maxChakra;}

    public static int getShinobiPoints() {
        return shinobi_points;
    }

    public static void setShinobi_points(int shinobi_points) {
        PlayerData.shinobi_points = shinobi_points;
    }

    public static String getClan() {
        return clan;
    }

    public static void setClan(String clan) {
        PlayerData.clan = clan;
    }

    public static String getRank() {
        return rank;
    }

    public static void setRank(String rank) {
        PlayerData.rank = rank;
    }

    public static String getReleaseList() {
        if (releaseList == null) {
            releaseList = ""; // or some default value
        }
        return releaseList;    }

    public static void setReleaseList(String releaseList) {
        PlayerData.releaseList = releaseList;
    }

    public static String getAffiliation() {
        return affiliation;
    }

    public static void setAffiliation(String affiliation) {
        PlayerData.affiliation = affiliation;
    }

    public static int getGenjutsu() {
        return genjutsu;
    }

    public static void setGenjutsu(int chakra) {
        PlayerData.genjutsu = chakra;
    }

    public static int getKenjutsu() {
        return kenjutsu;
    }

    public static void setKenjutsu(int kenjutsu) {
        PlayerData.kenjutsu = kenjutsu;
    }

    public static int getTaijutsu() {
        return taijutsu;
    }

    public static void setTaijutsu(int taijutsu) {
        PlayerData.taijutsu = taijutsu;
    }

    public static int getNinjutsu() {
        return ninjutsu;
    }

    public static void setNinjutsu(int ninjutsu) {
        PlayerData.ninjutsu = ninjutsu;
    }

    public static int getKinjutsu() {
        return kinjutsu;
    }

    public static void setKinjutsu(int kinjutsu) {
        PlayerData.kinjutsu = kinjutsu;
    }

    public static int getMedical() {
        return medical;
    }

    public static void setMedical(int medical) {
        PlayerData.medical = medical;
    }

    public static int getSenjutsu() {
        return senjutsu;
    }

    public static void setSenjutsu(int senjutsu) {
        PlayerData.senjutsu = senjutsu;
    }

    public static int getShurikenjutsu() {
        return shurikenjutsu;
    }

    public static void setShurikenjutsu(int shurikenjutsu) {
        PlayerData.shurikenjutsu = shurikenjutsu;
    }

    public static int getSpeed() {
        return speed;
    }

    public static void setSpeed(int speed) {
        PlayerData.speed = speed;
    }

    public static int getSummoning() {
        return summoning;
    }

    public static void setSummoning(int summoning) {
        PlayerData.summoning = summoning;
    }

    public static String getFireList() {
        return fireList;
    }

    public static void setFireList(String fireList) {
        PlayerData.fireList = fireList;
    }

    public static String getWaterList() {
        return waterList;
    }

    public static void setWaterList(String waterList) {
        PlayerData.waterList = waterList;
    }

    public static String getYangList() {
        return yangList;
    }

    public static void setYangList(String yangList) {
        PlayerData.yangList = yangList;
    }

    public static String getYinList() {
        return yinList;
    }

    public static void setYinList(String yinList) {
        PlayerData.yinList = yinList;
    }

    public static String getEarthList() {
        return earthList;
    }

    public static void setEarthList(String earthList) {
        PlayerData.earthList = earthList;
    }

    public static String getWindList() {
        return windList;
    }

    public static void setWindList(String windList) {
        PlayerData.windList = windList;
    }

    public static String getLightningList() {
        return lightningList;
    }

    public static void setLightningList(String lightningList) {
        PlayerData.lightningList = lightningList;
    }

    public static void setSelectedRelease(String release) {
        PlayerData.selectedRelease = release;
    }

    public static String getFireJutsu() {
        return fireJutsu;
    }

    public static void setFireJutsu(String fireJutsu) {
        PlayerData.fireJutsu = fireJutsu;
    }

    public static String getWaterJutsu() {
        return waterJutsu;
    }

    public static void setWaterJutsu(String waterJutsu) {
        PlayerData.waterJutsu = waterJutsu;
    }

    public static String getYangJutsu() {
        return yangJutsu;
    }

    public static void setYangJutsu(String yangJutsu) {
        PlayerData.yangJutsu = yangJutsu;
    }

    public static String getYinJutsu() {
        return yinJutsu;
    }

    public static void setYinJutsu(String yinJutsu) {
        PlayerData.yinJutsu = yinJutsu;
    }

    public static String getEarthJutsu() {
        return earthJutsu;
    }

    public static void setEarthJutsu(String earthJutsu) {
        PlayerData.earthJutsu = earthJutsu;
    }

    public static String getWindJutsu() {
        return windJutsu;
    }

    public static void setWindJutsu(String windJutsu) {
        PlayerData.windJutsu = windJutsu;
    }

    public static String getLightningJutsu() {
        return lightningJutsu;
    }

    public static void setLightningJutsu(String lightningJutsu) {
        PlayerData.lightningJutsu = lightningJutsu;
    }

   // public static boolean getChakraControl() {
   //     return chakraControl;
   // }

   // public static void setChakraControl(boolean chakraControl) {
   //     PlayerData.chakraControl = chakraControl;
   // }

   // public static String getSelectedRelease() {
   //     return selectedRelease;
   // }

   // public static void setSelectedRelease(String release) {
   //     PlayerData.selectedRelease = release;
   // }

}
