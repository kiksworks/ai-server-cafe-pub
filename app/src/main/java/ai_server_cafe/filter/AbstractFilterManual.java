package ai_server_cafe.filter;

import ai_server_cafe.model.field.AbstractFilteredObject;
import ai_server_cafe.util.interfaces.IFuncParam1;

import java.util.Optional;

/**
 * データの更新タイミングがvisionデータの更新タイミングと異なる
 * @param <T> Vをフィルタリングした結果
 * @param <V> フィルタをかける前
 */
public abstract class AbstractFilterManual<T extends AbstractFilteredObject<V>, V> extends AbstractFilter<T, V> implements IObserver<T> {
    protected IFuncParam1<Void, Optional<T>> writerFunc;

    public AbstractFilterManual() {
        this.writerFunc = new IFuncParam1<Void, Optional<T>>() {
            @Override
            public Void function(Optional<T> t) {
                return null;
            }
        };
    }

    public void setWriterFunc(IFuncParam1<Void, Optional<T>> writerFunc) {
        this.writerFunc = writerFunc;
    }

    /**
     * 取り込んだvisionデータと引数を組み合わせたものでデータを更新
     * @param args 引数(制御入力など)
     */
    public abstract void updateObserver(Object... args);

    /**
     * 生データを追加
     * @param value 生データ
     */
    protected void write(Optional<T> value) {
        this.writerFunc.function(value);
    }
}
