package ai_server_cafe.records;

/**
 * LayerDataの基底クラス．
 * 共通して使用するものはここに書く．
 */
public abstract class AbstractLayerData {
    // RegistryGraphicalComponentsで作成したlayerの名前
    public String name = "";

    // Graphics2Dのメソッドで描画する色
    public int[] color = {};
}
