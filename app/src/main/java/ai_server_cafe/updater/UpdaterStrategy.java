package ai_server_cafe.updater;

import ai_server_cafe.game.strategy.XGType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.WrapperWeakCloneableMap;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class UpdaterStrategy {
    private static UpdaterStrategy instance = null;
    private final double[] blueXGTypeA = new double[16];
    private final double[] yellowXGTypeA = new double[16];
    private final double[] blueXGTypeB = new double[16];
    private final double[] yellowXGTypeB = new double[16];
    private final double[] blueXGTypeC = new double[16];
    private final double[] yellowXGTypeC = new double[16];

    private int blueAttackside = 1;
    private int yellowAttackside = -1;
    // パスターゲット候補地点[候補点, スコア]
    private Map<Vector2D, Double> blueXGMapA;
    private Map<Vector2D, Double> yellowXGMapA;
    private Map<Vector2D, Double> blueXGMapB;
    private Map<Vector2D, Double> yellowXGMapB;
    private Map<Vector2D, Double> blueXGMapC;
    private Map<Vector2D, Double> yellowXGMapC;
    private final Logger logger = LogManager.getLogger("stats updater");

    private Map<Vector2D, Double> replayBlueXGMapA;
    private Map<Vector2D, Double> replayYellowXGMapA;
    private Map<Vector2D, Double> replayBlueXGMapB;
    private Map<Vector2D, Double> replayYellowXGMapB;
    private Map<Vector2D, Double> replayBlueXGMapC;
    private Map<Vector2D, Double> replayYellowXGMapC;

    private UpdaterStrategy() {
        Arrays.fill(this.blueXGTypeA, 0);
        Arrays.fill(this.yellowXGTypeA, 0);
        Arrays.fill(this.blueXGTypeB, 0);
        Arrays.fill(this.yellowXGTypeB, 0);
        Arrays.fill(this.blueXGTypeC, 0);
        Arrays.fill(this.yellowXGTypeC, 0);

        blueXGMapA = new HashMap<>();
        yellowXGMapA = new HashMap<>();
        blueXGMapB = new HashMap<>();
        yellowXGMapB = new HashMap<>();
        blueXGMapC = new HashMap<>();
        yellowXGMapC = new HashMap<>();

        replayBlueXGMapA = new HashMap<>();
        replayYellowXGMapA = new HashMap<>();
        replayBlueXGMapB = new HashMap<>();
        replayYellowXGMapB = new HashMap<>();
        replayBlueXGMapC = new HashMap<>();
        replayYellowXGMapC = new HashMap<>();
    }
    /**
     * インスタンスファクトリ
     * @return instance
     */
    public synchronized static UpdaterStrategy getInstance() {
        if (instance == null) {
            instance = new UpdaterStrategy();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    synchronized public void replayStop() {
        this.replayBlueXGMapA.clear();
        this.replayYellowXGMapA.clear();
        this.replayBlueXGMapB.clear();
        this.replayYellowXGMapB.clear();
        this.replayBlueXGMapC.clear();
        this.replayYellowXGMapC.clear();
    }

    synchronized public void resetXG(){
        Arrays.fill(this.blueXGTypeA, 0);
        Arrays.fill(this.yellowXGTypeA, 0);
        Arrays.fill(this.blueXGTypeB, 0);
        Arrays.fill(this.yellowXGTypeB, 0);
        Arrays.fill(this.blueXGTypeC, 0);
        Arrays.fill(this.yellowXGTypeC, 0);
    }

    /* ========================= 以下 setter =========================*/
    synchronized public void setXG(double count, TeamColor color, int id, XGType type) {
        if(type == XGType.TYPEA){
            if (color.isYellow()) {
                this.yellowXGTypeA[id] = count;
            } else {
                this.blueXGTypeA[id] = count;
            }
        }else if (type == XGType.TYPEB) {
            if (color.isYellow()) {
                this.yellowXGTypeB[id] = count;
            } else {
                this.blueXGTypeB[id] = count;
            }
        }else{
            if (color.isYellow()) {
                this.yellowXGTypeC[id] = count;
            } else {
                this.blueXGTypeC[id] = count;
            }
        }
    }

    synchronized public void setAttackDire(int side ,TeamColor color) {
        if (color.isYellow()) {
            this.yellowAttackside = side;
        } else {
            this.blueAttackside = side;
        }
    }

    synchronized public void setXGMap(Map<Vector2D, Double> map, TeamColor color, XGType type) {
        if(type == XGType.TYPEA){
            if (color.isYellow()) {
                this.yellowXGMapA = map;
            } else {
                this.blueXGMapA = map;
            }
        }else if (type == XGType.TYPEB) {
            if (color.isYellow()) {
                this.yellowXGMapB = map;
            } else {
                this.blueXGMapB = map;
            }
        }else{
            if (color.isYellow()) {
                this.yellowXGMapC = map;
            } else {
                this.blueXGMapC = map;
            }
        }
    }

    synchronized public void setReplayXGMap(Map<Vector2D, Double> map, @Nonnull TeamColor color, @Nonnull XGType type) {
        switch (type) {
            case TYPEA : {
                if(color.isYellow()) this.replayYellowXGMapA = map;
                else this.replayBlueXGMapA = map;
                break;
            }
            case TYPEB : {
                if(color.isYellow()) this.replayYellowXGMapB = map;
                else this.replayBlueXGMapB = map;
                break;
            }
            case TYPEC : {
                if(color.isYellow()) this.replayYellowXGMapC = map;
                else this.replayBlueXGMapC = map;
                break;
            }
        }
    }

    /* ========================= 以下 getter =========================*/
    synchronized public double getXG(TeamColor color, int id,XGType type) {
        if(type == XGType.TYPEA){
            if (color.isYellow()) {
                return this.yellowXGTypeA[id];
            } else {
                return this.blueXGTypeA[id];
            }
        }else if (type == XGType.TYPEB) {
            if (color.isYellow()) {
                return this.yellowXGTypeB[id];
            } else {
                return this.blueXGTypeB[id];
            }
        }else{
            if (color.isYellow()) {
                return this.yellowXGTypeC[id];
            } else {
                return this.blueXGTypeC[id];
            }
        }
    }

    synchronized public double getBestXG(TeamColor color, int id,XGType type) {
        if(type == XGType.TYPEA){
            if (color.isYellow()) {
                return this.yellowXGTypeA[id];
            } else {
                return this.blueXGTypeA[id];
            }
        }else if (type == XGType.TYPEB) {
            if (color.isYellow()) {
                return this.yellowXGTypeB[id];
            } else {
                return this.blueXGTypeB[id];
            }
        }else{
            if (color.isYellow()) {
                return this.yellowXGTypeC[id];
            } else {
                return this.blueXGTypeC[id];
            }
        }
    }

    synchronized public int getAttackDire(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowAttackside;
        } else {
            return this.blueAttackside;
        }
    }
    synchronized public WrapperWeakCloneableMap<Vector2D, Double> getXGMap(TeamColor color,XGType type) {
        final Map<Vector2D, Double> result;
        if(type == XGType.TYPEA){
            if (color.isYellow()) {
                result = (this.replayYellowXGMapA.isEmpty() ? this.yellowXGMapA : this.replayYellowXGMapA);
            } else {
                result = (this.replayBlueXGMapA.isEmpty() ? this.blueXGMapA : this.replayBlueXGMapA);
            }
        }else if (type == XGType.TYPEB) {
            if (color.isYellow()) {
                result = (this.replayYellowXGMapB.isEmpty() ? this.yellowXGMapB : this.replayYellowXGMapB);
            } else {
                result = (this.replayBlueXGMapB.isEmpty() ? this.blueXGMapB : this.replayBlueXGMapB);
            }
        }else{
            if (color.isYellow()) {
                result = (this.replayYellowXGMapC.isEmpty() ? this.yellowXGMapC : this.replayYellowXGMapC);
            } else {
                result = (this.replayBlueXGMapC.isEmpty() ? this.blueXGMapC : this.replayBlueXGMapC);
            }
        }
        return new WrapperWeakCloneableMap<>(result);
    }
}
