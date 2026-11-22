package net.beamex.shamaschizm.grimoire.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Native pixel geometry for physical leaves; independent of screen resolution. */
final class NerodemusPaper {
    static final int W=144,H=178;
    static boolean[] leafMask(boolean[] face,boolean[] reverse) {
        boolean[] result=face.clone();
        for(int y=0;y<H;y++)for(int x=0;x<W;x++)result[y*W+x]|=reverse[y*W+W-1-x];
        return result;
    }
    static boolean[] intersect(boolean[] visible,boolean[] holes,boolean through) {
        boolean[] result=new boolean[W*H];
        for(int i=0;i<result.length;i++)result[i]=visible[i]&&(holes[i]==through);
        return result;
    }
    /** Merge equal horizontal runs vertically: no per-pixel draw calls. */
    static List<int[]> rectangles(boolean[] mask) {
        List<int[]> result=new ArrayList<>();
        Map<Integer,int[]> previous=new HashMap<>();
        for(int y=0;y<H;y++) {
            Map<Integer,int[]> row=new HashMap<>();
            for(int x=0;x<W;) {
                if(!mask[y*W+x]){x++;continue;}
                int begin=x;while(x<W&&mask[y*W+x])x++;
                int key=begin*W+x-1;
                int[] rectangle=previous.get(key);
                if(rectangle==null){rectangle=new int[]{begin,y,x,y+1};result.add(rectangle);}
                else rectangle[3]=y+1;
                row.put(key,rectangle);
            }
            previous=row;
        }
        return result;
    }
    private NerodemusPaper() {}
}
