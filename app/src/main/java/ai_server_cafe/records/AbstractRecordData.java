package ai_server_cafe.records;

/**
 * RecordDataの基底クラス．
 * 共通して使用するものはここに書く
 */
public abstract class AbstractRecordData {
    /**
     * RecordDataのバージョン．
     * RecordData0は0
     * RecordData1は1
     */
    public int format = 0;

    // 保存するRecordDataが何フレーム目か
    public long frame = 0;

    // 保存するGameのcycleTime
    public double cycleTime = 0;

    public boolean isGoalOfYellowPositive = false;

    public abstract boolean matchFormat();
}
