package ai_server_cafe.game.planner.path;

import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.IFuncParam2;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.Optional;

public enum EnumPathPlannerType {
    BREADTH_FIRST(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new BreadthFirst().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            0, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstra().getScore(pathSides, vector2D, true);
        }
    }),
    HUMAN_LIKE(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new HumanLike().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            data.additionalDepth, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstra().getScore(pathSides, vector2D, true);
        }
    }),
    DIJKSTRA(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new Dijkstra().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            0, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstra().getScore(pathSides, vector2D, true);
        }
    }),
    ROUND_ROBIN(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new RoundRobin().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            data.additionalDepth, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstra().getScore(pathSides, vector2D, true);
        }
    }),
    TIME_BELLMAN_FORD(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new TimeBellmanFord().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            data.additionalDepth, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstra().getScore(pathSides, vector2D, true);
        }
    }),
    /**
     * JapanOpen2025で使ったもの
     */
    TIME_DIJKSTRA_LQ(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new TimeDijkstra().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            0, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstra().getScore(pathSides, vector2D, true);
        }
    }),
    TIME_DIJKSTRA_HQ(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new TimeDijkstraHighQuality().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            0, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstraB().getScore(pathSides, vector2D, true);
        }
    }),
    TIME_DIJKSTRA_B(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new TimeDijkstraB().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            0, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new TimeDijkstraB().getScore(pathSides, vector2D, true);
        }
    }),
    BELLMAN_FORD(new IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet>() {
        @Override
        public Optional<Pair<List<PathSide>, Vector2D>> function(DataSet data) {
            return new BellmanFord().setId(data.id)
                    .getFinalPath(data.start, data.goal, data.obstacles, data.step, data.depth, data.allowObstacleEnd,
                            0, data.vel);
        }
    }, new IFuncParam2<Double, List<PathSide>, Optional<Vector2D>>() {
        @Override
        public Double function(List<PathSide> pathSides, Optional<Vector2D> vector2D) {
            return new BellmanFord().getScore(pathSides, vector2D);
        }
    });

    private final IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet> runningFunc;
    private final IFuncParam2<Double, List<PathSide>, Optional<Vector2D>> scoreFunc;

    EnumPathPlannerType(IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet> runningFunc,
            IFuncParam2<Double, List<PathSide>, Optional<Vector2D>> scoreFunc) {
        this.runningFunc = runningFunc;
        this.scoreFunc = scoreFunc;
    }

    public IFuncParam1<Optional<Pair<List<PathSide>, Vector2D>>, DataSet> getCalculator() {
        return this.runningFunc;
    }

    public IFuncParam2<Double, List<PathSide>, Optional<Vector2D>> getScoreFunc() {
        return this.scoreFunc;
    }
}
