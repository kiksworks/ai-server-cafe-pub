package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleKickRegulator;
import ai_server_cafe.game.role.RolePerformance;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.KickManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormationKickRegulator extends AbstractFormation {
    private int size;
    private int currentIndex;
    private List<Pair<Integer, Integer>> regulatorPairList;
    private boolean isFinished;
    private final RoleKickRegulator roleKickRegulator;
    private final RolePerformance rolePerformance;
    private static final Logger LOGGER = LogManager.getLogger("kick-regulator");

    public FormationKickRegulator(TeamColor color, int[] ids) {
        super(color, ids);
        this.currentIndex = 0;
        this.regulatorPairList = new ArrayList<>();
        this.isFinished = true;
        this.roleKickRegulator = new RoleKickRegulator(color, ids);
        this.rolePerformance = new RolePerformance(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;
        final List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        if(visibleIds.isEmpty()) return list;

        // ロボットの振り分け
        if(this.roleKickRegulator.isAllFinished(visibleIds)) {
            // 全台終わったらパフォーマンスをする
            this.rolePerformance.setRoleIds(visibleIds);
            list.add(this.rolePerformance);
            return list;
        }

        if(this.regulatorPairList.isEmpty()) {
            this.size = visibleIds.size();
            this.regulatorPairList = this.makeRegulatorPairList(visibleIds);
            LOGGER.info("make pair : {}", this.regulatorPairList);
        }

        if(visibleIds.size() < this.size) {
            // ロボットが見えなくなったらhalt
            // ロボットが追加された場合はレギュレータ続行
            LOGGER.info("robot is lost.");
            this.roleHalt.setRoleIds(visibleIds);
            list.add(this.roleHalt);
            return list;
        }

        if (this.isFinished) {
            // reset regulator
            this.roleKickRegulator.reset();
            this.roleWaiter.setRoleIds(visibleIds);
            this.roleKickRegulator.setRoleIds(List.of());
            this.isFinished = false;
        } else if(!this.roleKickRegulator.isAllFinished(visibleIds)) {
            // regulator
            this.roleKickRegulator.setRoleIds(
                    this.convertListFromPair(this.regulatorPairList.get(this.currentIndex)));

            // waiter
            final List<Integer> waiterIds = new ArrayList<>(visibleIds);
            waiterIds.removeAll(this.convertListFromPair(this.regulatorPairList.get(this.currentIndex)));
            Map<Integer, Vector2D> waitPositions = new HashMap<>();
            if(this.world.get().getField().getGameHeight() >= 3000.0) {
                // divisionA
                // divisionB
                this.roleKickRegulator.setInitPosition(
                        new Vector2D(-2500.0, -2000.0),
                        new Vector2D(-2500.0, 2000.0));
                for (int id = 0; id < ConfigManager.MAX_ROBOTS; id++) {
                    waitPositions.put(id, new Vector2D(-500.0, id * 220.0 - visibleIds.size() * 100.0));
                }
            }
            this.roleWaiter.setWaitPositionsWithId(waitPositions);
            this.roleWaiter.setAvoidBall(true);
            this.roleWaiter.setRoleIds(waiterIds);
        }
        this.isFinished = this.roleKickRegulator.isFinished();

        // regulatorの記録
        if (this.isFinished) {
            Map<Integer, List<Pair<Integer, Double>>> results = this.roleKickRegulator.getResult();
            for(Integer id : this.convertListFromPair(this.regulatorPairList.get(this.currentIndex))) {
                List<Pair<Integer, Double>> result = results.get(id);
                if(!results.containsKey(id) || result.isEmpty()) continue;
                // 2次の最小二乗法を行う
                double[] y = new double[result.size()];
                RealMatrix matrix = MathHelper.makeFill(result.size(), 3, 0.0);
                for(int i = 0; i < result.size(); i++) {
                    matrix.setEntry(i, 0, result.get(i).getFirst() * result.get(i).getFirst());
                    matrix.setEntry(i, 1, result.get(i).getFirst());
                    matrix.setEntry(i, 2, 1);
                    y[i] = result.get(i).getSecond();
                }
                // c2, c1, c0
                double[] cx = {};
                cx = MathHelper.getSVDInverseMatrix(matrix).multiply(
                        MathHelper.makeVectorMatrix(y)).getColumn(0);
                KickManager.getInstance().setKickParam(id, false, cx[2], cx[1], cx[0]);

                // 結果の表示
                String resultLog = "[";
                // 小数点第2位以下切り捨て
                for(int i = 0; i < result.size(); i++) {
                    final Pair<Integer, Double> pair = result.get(i);
                    resultLog = resultLog.concat("[" + pair.getFirst() + ","
                            + String.format("%.2f", pair.getSecond()) + "]");
                    if(i + 1 < result.size()) resultLog = resultLog.concat(",");
                }
                resultLog = resultLog.concat("]");
                LOGGER.info("{}:{} data is {}.", this.color, id, resultLog);

                String cxLog = "[";
                // 小数点第2位以下切り捨て
                for(int i = 2; i >= 0; i--) {
                    cxLog = cxLog.concat("cx[" + i + "]:");
                    cxLog = cxLog.concat(String.format("%.2f", cx[i]));
                    if(i > 0) cxLog = cxLog.concat(",");
                }
                cxLog = cxLog.concat("]");
                LOGGER.info("{}:{} result is {}", this.color, id, cxLog);
                this.roleKickRegulator.getResult().get(id).clear();
            }
            this.currentIndex++;
        }
        list.add(this.roleKickRegulator);
        list.add(this.roleWaiter);
        return list;
    }

    @Override
    public String getName() {
        return "kick_regulator";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    private List<Integer> convertListFromPair(Pair<Integer, Integer> pair) {
        List<Integer> result = new ArrayList<>();
        result.add(pair.getFirst());
        result.add(pair.getSecond());
        return result;
    }

    /**
     * @return listsからレギュレータのペアを生成する
     */
    private List<Pair<Integer, Integer>> makeRegulatorPairList(List<Integer> lists) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        List<Integer> tmpIds = new ArrayList<>(lists);
        if(tmpIds.size() == 1) {
            // ぼっちレギュレータ
            result.add(new Pair<>(tmpIds.getFirst(), tmpIds.getFirst()));
            return result;
        }
        for(int i = 0; i < tmpIds.size(); i += 2) {
            if(i >= tmpIds.size() - 1 && tmpIds.size() % 2 == 1) {
                // tmpIdsが奇数台のとき
                result.add(new Pair<>(tmpIds.getLast(), tmpIds.getFirst()));
            } else {
                // 通常のペア
                result.add(new Pair<>(tmpIds.get(i), tmpIds.get(i + 1)));
            }
        }
        return result;
    }
}
