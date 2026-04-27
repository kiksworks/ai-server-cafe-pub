package ai_server_cafe.util.math;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.FastMath;

public class CompensatedSingularValueDecomposition {
    private final RealMatrix V;
    private final RealMatrix U;
    private final RealMatrix S;

    public CompensatedSingularValueDecomposition(RealMatrix matrix) {
        SingularValueDecomposition singular = new SingularValueDecomposition(matrix);
        this.V = MathHelper.getCompensated(singular.getV());
        RealMatrix S = MathHelper.makeFill(matrix.getRowDimension(), matrix.getColumnDimension(), 0.0);
        for (int i = 0; i < FastMath.min(S.getRowDimension(), S.getColumnDimension()); i++) {
            S.setEntry(i, i, singular.getSingularValues()[i]);
        }
        this.S = S;
        this.U = MathHelper.getCompensated(singular.getU());
    }

    public RealMatrix getV() {
        return V;
    }

    public RealMatrix getU() {
        return U;
    }

    public RealMatrix getS() {
        return S;
    }
}
