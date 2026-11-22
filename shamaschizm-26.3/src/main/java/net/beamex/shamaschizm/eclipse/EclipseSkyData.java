package net.beamex.shamaschizm.eclipse;
/** One physical Sun and Moon per rendered view, including remote camera views. */
public interface EclipseSkyData {
    record View(boolean enabled,EclipseOrbit.Sky sky,float r,float g,float b) {
        public static final View NONE=new View(false,null,0,0,0);
    }
    View shamaschizm$eclipse();
    void shamaschizm$eclipse(View view);
}
