package ai_server_cafe.util.interfaces;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

/**
 * Cafeのクラスをserialize/deserialize可能にするクラス
 */
public interface SerializableJson<T> {
    public T deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException;

    /**
     * @implNote Cafeのクラスをserializeする処理を記述する
     */
    public JsonObject serialize();

    /**
     *
     * @param serialize SerializableJsonを継承したクラス
     * @return SerializableJson継承クラスのpath(クラスが存在するパッケージの場所)と
     * SerializableJson継承クラスのdata(上記のserialize()メソッドの戻り値)をJsonObjectに変換する
     */
    public static <T> JsonObject serializeJson(SerializableJson<T> serialize) {
        JsonObject result = new JsonObject();
        result.addProperty("path", serialize.getClass().getCanonicalName());
        result.add("data", serialize.serialize());
        return result;
    }

    /**
     * @implNote Cafeのクラスをdeserializeする処理を記述する
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T deserializeJson(@Nonnull JsonObject jsonObject) {
        if(jsonObject.has("path")) {
            String path = jsonObject.get("path").getAsString();
            try {
                // pathを使ってクラスを特定する
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(path);
                Object obj = clazz.getConstructor().newInstance();
                if (obj instanceof SerializableJson<?>) {
                    // dataを使ってインスタンスを生成(復元)する
                    return (T) ((SerializableJson<?>) obj).deserialize(jsonObject.get("data").getAsJsonObject());
                }
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                     | IllegalAccessException | InvocationTargetException | ClassCastException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
