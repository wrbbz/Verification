import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BuildFigure extends JPanel {
    private List<GraphElement> elementList;
    private List<Block> blocks;
    private Shape prevShape;
    private AbstractMap<ASTEntry, Shape> shapes = new HashMap<>();
    private int width = 150;
    private int height = 50;
    private Graphics2D g2;

    public BuildFigure(List<GraphElement> elementList, List<Block> blocks){
        this.elementList = elementList;
        this.blocks = blocks;
    }

    @Override
    public void paint(Graphics g) {
        final BufferedImage bi = new BufferedImage(1000, 3000, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();

        Point start_with = new Point(width, height);
        Point prev_elem_end = new Point(0, 0);
        Point block_elem = new Point(0, 0);

        g2.setPaint(Color.BLACK);

        for(GraphElement element : elementList) {
            if(ElementShape.ELLIPSE.equals(element.getElementShape())) {
                Ellipse2D ellipse = new Ellipse2D.Double(start_with.x, start_with.y, width, height);
                g2.drawString(element.getNode().text, (float) (ellipse.getCenterX() - element.getNode().text.length() * 3), (float) ellipse.getCenterY());
                g2.draw(ellipse);
                prev_elem_end.setLocation(ellipse.getMaxX(), ellipse.getMaxY() + 50);
                shapes.put(element.getNode(), ellipse);
                prevShape = ellipse;

            }
            else if(ElementShape.SQUARE.equals(element.getElementShape())){
                if(inBlock(element.getNode()))
                    continue;

                prev_elem_end = drawSquare(new Point(start_with.x, prev_elem_end.y), element.getNode());

            }
            else {
                Block block = getBlockWithStart(element.getNode());
                if(block == null) continue;
                if(block.isDrawn()) continue;

                int x = start_with.x;
                int y = prev_elem_end.y;

                prev_elem_end = drawDiamond(new Point(x, y), element.getNode());

                block_elem.setLocation(start_with.x + 2 * width, y);
                blocks.get(blocks.indexOf(block)).setDrawn(true);
                drawBlock(blocks.indexOf(block), block.getNodes().indexOf(block.getStartsWith()), block_elem);
            }
        }
        try{
            ImageIO.write(bi,"png",new File("test.png"));}catch (Exception e) {}
        for(Block block : blocks){
            block.setDrawn(false);
        }
    }

    private Point drawDiamond(Point drawPoint, ASTEntry node){
        Diamond diam = new Diamond(drawPoint.x,  drawPoint.y, width, height);
        AffineTransform at = AffineTransform.getTranslateInstance(drawPoint.x, drawPoint.y);
        Shape diamond = at.createTransformedShape(diam);
        g2.drawString(node.text, (float) (diamond.getBounds().getCenterX() - node.text.length() * 3), (float) diamond.getBounds().getCenterY());
        g2.draw(diamond);

        int x = (int)prevShape.getBounds().getCenterX();
        int y = (int)prevShape.getBounds().getMaxY();

        if((x - width / 2) == diamond.getBounds().getX()) {
            g2.drawLine(x, y, (int) diamond.getBounds().getCenterX(), (int) diamond.getBounds().getMinY());
        }
        else{
            findUpper(diamond);
        }

        prevShape = diamond;
        shapes.put(node, diamond);
        return new Point((int)diamond.getBounds().getMaxX(), (int)diamond.getBounds().getMaxY() + 50);
    }

    private Point drawSquare(Point drawPoint, ASTEntry node){
        Rectangle rectangle = new Rectangle(drawPoint.x, drawPoint.y, width, height);
        g2.drawString(node.text, (float) (rectangle.getCenterX() - node.text.length() * 3), (float) rectangle.getCenterY());
        g2.draw(rectangle);
        int x = (int)prevShape.getBounds().getCenterX();
        int y = (int)prevShape.getBounds().getMaxY();
        if(y - height == rectangle.getY()) {
            x = (int) prevShape.getBounds().getMaxX();
            y = (int) prevShape.getBounds().getCenterY();
            g2.drawLine(x, y, (int) rectangle.getMinX(), (int) rectangle.getCenterY());
            g2.drawString("True", x + width / 2 - "True".length() * 3, y - 1);
        }
        else if(x - width /2 != rectangle.getMinX()){
            findUpper(rectangle);
        }
        else
            g2.drawLine(x, y, (int)rectangle.getCenterX(), (int)rectangle.getMinY());
        prevShape = rectangle;
        shapes.put(node, rectangle);
        return new Point((int)rectangle.getX(), (int)rectangle.getMaxY() + 50);
    }

    private void findUpper(Shape figure){
        ArrayList<Shape> shapesList = new ArrayList<>(shapes.values());
        int index = shapesList.indexOf(prevShape);
        for(int i = 0; i < index; i++){
            int x = (int)shapesList.get(i).getBounds().getX();
            int y = (int)shapesList.get(i).getBounds().getY();
            if(x == figure.getBounds().getX() && (figure.getBounds().getY() - y <= 100))
                g2.drawLine(x + width / 2, y + height, (int)figure.getBounds().getCenterX(), (int)figure.getBounds().getMinY());

        }
    }
    private void drawBlock(int listIndex, int nodeIndex, Point drawPoint){
        Point blockPoint = drawPoint;
        List<GraphElement> blockElems = blocks.get(listIndex).getBlock();
        for(int i = nodeIndex + 1; i < blockElems.size(); i++){
            if(ElementShape.SQUARE.equals(blockElems.get(i).getElementShape())) {
                blockPoint = drawSquare(blockPoint, blockElems.get(i).getNode());
                drawEndLoop(blocks.get(listIndex), blockElems.get(i).getNode(), blockPoint);
            }
            else if(ElementShape.DIAMOND.equals(blockElems.get(i).getElementShape())) {
                drawPoint = drawDiamond(blockPoint, blockElems.get(i).getNode());
                blockPoint.setLocation(blockPoint.x, blockPoint.y + 100);
                drawPoint.setLocation(drawPoint.x + width, drawPoint.y - 100);
                Block block = getBlockWithStart(blockElems.get(i).getNode());
                blocks.get(blocks.indexOf(block)).setDrawn(true);
                drawBlock(blocks.indexOf(block), block.getNodes().indexOf(block.getStartsWith()), drawPoint);
            }
        }
    }

    private void drawEndLoop(Block block, ASTEntry node, Point startPoint){
        if(!node.equals(block.getEndsWith())) return;
        Point finalPoint = new Point(0, 0);

        for(ASTEntry startNode : shapes.keySet()) {
            if (startNode.equals(block.getStartsWith())) {
                Rectangle bounds = shapes.get(startNode).getBounds();
                finalPoint = new Point((int)bounds.getCenterX(), (int)bounds.getMinY());
                break;
            }
        }

        startPoint.y = startPoint.y - (int)(height * 1.5);
        g2.drawLine(startPoint.x, startPoint.y, finalPoint.x - width, startPoint.y);
        startPoint.setLocation(finalPoint.x - width, startPoint.y);
        g2.drawLine(startPoint.x, startPoint.y, startPoint.x, finalPoint.y - height / 2);
        startPoint.setLocation(startPoint.x, finalPoint.y - height / 2);
        g2.drawLine(startPoint.x, startPoint.y, finalPoint.x, startPoint.y);
    }

    private Block getBlockWithStart(ASTEntry startsWith){
        for(Block block: blocks)
            if(block.getStartsWith().equals(startsWith))
                return block;
        return null;
    }

    private boolean inBlock(ASTEntry elem){
        for(Block block : blocks){
            if(block.getNodes().contains(elem))
                return true;
        }
        return false;
    }

    public class Diamond extends Path2D.Double{

        private int x;
        private int y;
        private double width;
        private double height;

        public Diamond(int x, int y, double width, double height){
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            create();
        }

        private void create(){
            moveTo(0, height / 2);
            lineTo(width / 2, 0);
            lineTo(width, height / 2);
            lineTo(width / 2, height);
            closePath();
        }
    }
}
