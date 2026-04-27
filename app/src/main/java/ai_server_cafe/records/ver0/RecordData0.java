package ai_server_cafe.records.ver0;

import ai_server_cafe.records.AbstractLayerData;
import ai_server_cafe.records.AbstractRecordData;

/**
 * GameのGraphics2Dを保存するクラス．
 * ObstacleやWorldのデータなどは保存できない．
 */
public class RecordData0 extends AbstractRecordData {
    // 保存するGameのフィールドの高さおよび幅
    public double drawHeight = 0;
    public double drawWidth = 0;
    public LayerData0[] layerData0 = {};

    @Override
    public boolean matchFormat() {
        return (this.format == 0);
    }

    public static class LayerData0 extends AbstractLayerData {
        public TypeData type = new TypeData();
    }

    /**
     * Cafeでよく使用するGraphics2Dを上手い感じに保存するクラス
     * @see GraphicsRecord
     */
    public static class TypeData {
        public String type = "";
        public String str = "";
        public String fontName = "";
        public int fontType = 0;
        public int fontSize = 0;
        public int[] ax = {};
        public int[] ay = {};
        public double[] ad = {};
        public int nPoints = 0;
        public double[] affineMatrix = {};
    }
}
