package com.deepsea.stratagemhero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Generated from design/catalog.json. Do not edit by hand. */
public final class StratagemCatalog {
    private StratagemCatalog() {}

    public static final String MISSION = "MISSION";
    public static final String SUPPORT = "SUPPORT";
    public static final String OFFENSIVE = "OFFENSIVE";
    public static final String BACKPACK = "BACKPACK";
    public static final String SENTRY = "SENTRY";
    public static final String DEFENSIVE = "DEFENSIVE";

    public static final List<Stratagem> ALL;
    static {
        List<Stratagem> list = new ArrayList<>();
        list.add(new Stratagem("reinforce", "Reinforce", "增援", MISSION, "UDRLU", "reinforce"));
        list.add(new Stratagem("resupply", "Resupply", "补给", MISSION, "DDUR", "resupply"));
        list.add(new Stratagem("sos_beacon", "SOS Beacon", "SOS 信标", MISSION, "UDRU", "sos_beacon"));
        list.add(new Stratagem("machine_gun", "MG-43 Machine Gun", "MG-43 机枪", SUPPORT, "DLDUR", "machine_gun"));
        list.add(new Stratagem("amr", "APW-1 Anti-Materiel Rifle", "APW-1 反器材步枪", SUPPORT, "DLRUD", "amr"));
        list.add(new Stratagem("stalwart", "M-105 Stalwart", "M-105 坚定者", SUPPORT, "DLDUUL", "stalwart"));
        list.add(new Stratagem("eat", "EAT-17 Expendable Anti-Tank", "EAT-17 一次性反坦克武器", SUPPORT, "DDLUR", "eat"));
        list.add(new Stratagem("recoilless", "GR-8 Recoilless Rifle", "GR-8 无后坐力炮", SUPPORT, "DLRRL", "recoilless"));
        list.add(new Stratagem("flamethrower", "FLAM-40 Flamethrower", "FLAM-40 火焰喷射器", SUPPORT, "DLUDU", "flamethrower"));
        list.add(new Stratagem("autocannon", "AC-8 Autocannon", "AC-8 自动加农炮", SUPPORT, "DLDUUR", "autocannon"));
        list.add(new Stratagem("hmg", "MG-206 Heavy Machine Gun", "MG-206 重机枪", SUPPORT, "DLUDD", "hmg"));
        list.add(new Stratagem("railgun", "RS-422 Railgun", "RS-422 磁轨炮", SUPPORT, "DRDULR", "railgun"));
        list.add(new Stratagem("spear", "FAF-14 Spear", "FAF-14 长矛导弹", SUPPORT, "DDUDD", "spear"));
        list.add(new Stratagem("commando", "MLS-4X Commando", "MLS-4X 突击兵", SUPPORT, "DLUDR", "commando"));
        list.add(new Stratagem("airburst_rocket", "RL-77 Airburst Rocket Launcher", "RL-77 空爆火箭发射器", SUPPORT, "DUULR", "airburst_rocket"));
        list.add(new Stratagem("eagle_500", "Eagle 500kg Bomb", "飞鹰 500kg 炸弹", OFFENSIVE, "URDDD", "eagle_500"));
        list.add(new Stratagem("eagle_airstrike", "Eagle Airstrike", "飞鹰空袭", OFFENSIVE, "URDR", "eagle_airstrike"));
        list.add(new Stratagem("eagle_cluster", "Eagle Cluster Bomb", "飞鹰集束炸弹", OFFENSIVE, "URDDR", "eagle_cluster"));
        list.add(new Stratagem("eagle_napalm", "Eagle Napalm Airstrike", "飞鹰凝固汽油弹空袭", OFFENSIVE, "URDU", "eagle_napalm"));
        list.add(new Stratagem("eagle_smoke", "Eagle Smoke Strike", "飞鹰烟雾攻击", OFFENSIVE, "URUD", "eagle_smoke"));
        list.add(new Stratagem("eagle_110", "Eagle 110mm Rocket Pods", "飞鹰 110mm 火箭巢", OFFENSIVE, "URUL", "eagle_110"));
        list.add(new Stratagem("eagle_strafing", "Eagle Strafing Run", "飞鹰机枪扫射", OFFENSIVE, "URR", "eagle_strafing"));
        list.add(new Stratagem("orbital_gatling", "Orbital Gatling Barrage", "轨道加特林火力网", OFFENSIVE, "RDLUU", "orbital_gatling"));
        list.add(new Stratagem("orbital_airburst", "Orbital Airburst Strike", "轨道空爆攻击", OFFENSIVE, "RRR", "orbital_airburst"));
        list.add(new Stratagem("orbital_120", "Orbital 120mm HE Barrage", "轨道 120mm 高爆火力网", OFFENSIVE, "RRDLRD", "orbital_120"));
        list.add(new Stratagem("orbital_380", "Orbital 380mm HE Barrage", "轨道 380mm 高爆火力网", OFFENSIVE, "RDUULDD", "orbital_380"));
        list.add(new Stratagem("orbital_walking", "Orbital Walking Barrage", "轨道游走火力网", OFFENSIVE, "RDRDRD", "orbital_walking"));
        list.add(new Stratagem("orbital_laser", "Orbital Laser", "轨道激光", OFFENSIVE, "RDURD", "orbital_laser"));
        list.add(new Stratagem("orbital_railcannon", "Orbital Railcannon Strike", "轨道磁轨炮攻击", OFFENSIVE, "RUDDR", "orbital_railcannon"));
        list.add(new Stratagem("orbital_napalm", "Orbital Napalm Barrage", "轨道凝固汽油弹火力网", OFFENSIVE, "RRDLRU", "orbital_napalm"));
        list.add(new Stratagem("jump_pack", "LIFT-850 Jump Pack", "LIFT-850 跳跃背包", BACKPACK, "DUUDU", "jump_pack"));
        list.add(new Stratagem("supply_pack", "B-1 Supply Pack", "B-1 补给背包", BACKPACK, "DLDUUD", "supply_pack"));
        list.add(new Stratagem("shield_pack", "SH-32 Shield Generator Pack", "SH-32 护盾生成器背包", BACKPACK, "DULRLR", "shield_pack"));
        list.add(new Stratagem("guard_dog", "AX/AR-23 Guard Dog", "AX/AR-23 护卫犬", BACKPACK, "DULURD", "guard_dog"));
        list.add(new Stratagem("guard_dog_rover", "AX/LAS-5 Guard Dog Rover", "AX/LAS-5 护卫犬漫游者", BACKPACK, "DULURR", "guard_dog_rover"));
        list.add(new Stratagem("ballistic_shield", "SH-20 Ballistic Shield Backpack", "SH-20 防弹盾背包", BACKPACK, "DLDDUL", "ballistic_shield"));
        list.add(new Stratagem("mg_sentry", "A/MG-43 Machine Gun Sentry", "A/MG-43 机枪哨戒炮", SENTRY, "DURRU", "mg_sentry"));
        list.add(new Stratagem("gatling_sentry", "A/G-16 Gatling Sentry", "A/G-16 加特林哨戒炮", SENTRY, "DURL", "gatling_sentry"));
        list.add(new Stratagem("mortar_sentry", "A/M-12 Mortar Sentry", "A/M-12 迫击炮哨戒炮", SENTRY, "DURRD", "mortar_sentry"));
        list.add(new Stratagem("ac_sentry", "A/AC-8 Autocannon Sentry", "A/AC-8 自动加农炮哨戒炮", SENTRY, "DURULU", "ac_sentry"));
        list.add(new Stratagem("rocket_sentry", "A/MLS-4X Rocket Sentry", "A/MLS-4X 火箭哨戒炮", SENTRY, "DURRL", "rocket_sentry"));
        list.add(new Stratagem("ems_mortar", "A/M-23 EMS Mortar Sentry", "A/M-23 EMS 迫击炮哨戒炮", SENTRY, "DURDR", "ems_mortar"));
        list.add(new Stratagem("hmg_emplacement", "E/MG-101 HMG Emplacement", "E/MG-101 重机枪阵地", DEFENSIVE, "DULRRL", "hmg_emplacement"));
        list.add(new Stratagem("shield_relay", "FX-12 Shield Generator Relay", "FX-12 护盾生成器中继", DEFENSIVE, "DDLRLR", "shield_relay"));
        list.add(new Stratagem("tesla_tower", "A/ARC-3 Tesla Tower", "A/ARC-3 特斯拉塔", DEFENSIVE, "DURULR", "tesla_tower"));
        ALL = Collections.unmodifiableList(list);
    }

    public static Stratagem find(String id) {
        for (Stratagem s : ALL) if (s.id.equals(id)) return s;
        return null;
    }
}
