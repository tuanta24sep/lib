package zetcodeJavaSwing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import java.util.Random;

import zetcodeJavaSwing.Shape.Tetrominoes;

public class Tetris extends JFrame {

	private JLabel statusbar;

	public Tetris() {

		initUI();
	}

	private void initUI() {

		statusbar = new JLabel(" 0");
		add(statusbar, BorderLayout.SOUTH);
		Board board = new Board(this);
		add(board);
		board.start();

		setSize(200, 400);
		setTitle("Tetris");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
	}

	public JLabel getStatusBar() {

		return statusbar;
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				Tetris game = new Tetris();
				game.setVisible(true);
			}
		});
	}
}

class Shape {

	protected enum Tetrominoes {
		NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, MirroredLShape
	};

	private Tetrominoes pieceShape;
	private int coords[][];
	private int[][][] coordsTable;

	public Shape() {

		coords = new int[4][2];
		setShape(Tetrominoes.NoShape);
	}

	public void setShape(Tetrominoes shape) {

		coordsTable = new int[][][] {
				{ { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
				{ { 0, -1 }, { 0, 0 }, { -1, 0 }, { -1, 1 } },
				{ { 0, -1 }, { 0, 0 }, { 1, 0 }, { 1, 1 } },
				{ { 0, -1 }, { 0, 0 }, { 0, 1 }, { 0, 2 } },
				{ { -1, 0 }, { 0, 0 }, { 1, 0 }, { 0, 1 } },
				{ { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } },
				{ { -1, -1 }, { 0, -1 }, { 0, 0 }, { 0, 1 } },
				{ { 1, -1 }, { 0, -1 }, { 0, 0 }, { 0, 1 } } };

		for (int i = 0; i < 4; i++) {

			for (int j = 0; j < 2; ++j) {

				coords[i][j] = coordsTable[shape.ordinal()][i][j];
			}
		}

		pieceShape = shape;
	}

	private void setX(int index, int x) {
		coords[index][0] = x;
	}

	private void setY(int index, int y) {
		coords[index][1] = y;
	}

	public int x(int index) {
		return coords[index][0];
	}

	public int y(int index) {
		return coords[index][1];
	}

	public Tetrominoes getShape() {
		return pieceShape;
	}

	public void setRandomShape() {

		Random r = new Random();
		int x = Math.abs(r.nextInt()) % 7 + 1;
		Tetrominoes[] values = Tetrominoes.values();
		setShape(values[x]);
	}

	public int minX() {

		int m = coords[0][0];

		for (int i = 0; i < 4; i++) {

			m = Math.min(m, coords[i][0]);
		}

		return m;
	}

	public int minY() {

		int m = coords[0][1];

		for (int i = 0; i < 4; i++) {

			m = Math.min(m, coords[i][1]);
		}

		return m;
	}

	public Shape rotateLeft() {

		if (pieceShape == Tetrominoes.SquareShape)
			return this;

		Shape result = new Shape();
		result.pieceShape = pieceShape;

		for (int i = 0; i < 4; ++i) {

			result.setX(i, y(i));
			result.setY(i, -x(i));
		}

		return result;
	}

	public Shape rotateRight() {

		if (pieceShape == Tetrominoes.SquareShape)
			return this;

		Shape result = new Shape();
		result.pieceShape = pieceShape;

		for (int i = 0; i < 4; ++i) {

			result.setX(i, -y(i));
			result.setY(i, x(i));
		}

		return result;
	}
}

class Board extends JPanel implements ActionListener {

	private final int BoardWidth = 10;
	private final int BoardHeight = 22;

	private Timer timer;
	private boolean isFallingFinished = false;
	private boolean isStarted = false;
	private boolean isPaused = false;
	private int numLinesRemoved = 0;
	private int curX = 0;
	private int curY = 0;
	private JLabel statusbar;
	private Shape curPiece;
	private Tetrominoes[] board;

	public Board(Tetris parent) {

		initBoard(parent);
	}

	private void initBoard(Tetris parent) {

		setFocusable(true);
		curPiece = new Shape();
		timer = new Timer(400, this);
		timer.start();

		statusbar = parent.getStatusBar();
		board = new Tetrominoes[BoardWidth * BoardHeight];
		addKeyListener(new TAdapter());
		clearBoard();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (isFallingFinished) {

			isFallingFinished = false;
			newPiece();
		} else {

			oneLineDown();
		}
	}

	private int squareWidth() {
		return (int) getSize().getWidth() / BoardWidth;
	}

	private int squareHeight() {
		return (int) getSize().getHeight() / BoardHeight;
	}

	private Tetrominoes shapeAt(int x, int y) {
		return board[(y * BoardWidth) + x];
	}

	public void start() {

		if (isPaused)
			return;

		isStarted = true;
		isFallingFinished = false;
		numLinesRemoved = 0;
		clearBoard();

		newPiece();
		timer.start();
	}

	private void pause() {

		if (!isStarted)
			return;

		isPaused = !isPaused;

		if (isPaused) {

			timer.stop();
			statusbar.setText("paused");
		} else {

			timer.start();
			statusbar.setText(String.valueOf(numLinesRemoved));
		}

		repaint();
	}

	private void doDrawing(Graphics g) {

		Dimension size = getSize();
		int boardTop = (int) size.getHeight() - BoardHeight * squareHeight();

		for (int i = 0; i < BoardHeight; ++i) {

			for (int j = 0; j < BoardWidth; ++j) {

				Tetrominoes shape = shapeAt(j, BoardHeight - i - 1);

				if (shape != Tetrominoes.NoShape)
					drawSquare(g, 0 + j * squareWidth(), boardTop + i
							* squareHeight(), shape);
			}
		}

		if (curPiece.getShape() != Tetrominoes.NoShape) {

			for (int i = 0; i < 4; ++i) {

				int x = curX + curPiece.x(i);
				int y = curY - curPiece.y(i);
				drawSquare(g, 0 + x * squareWidth(), boardTop
						+ (BoardHeight - y - 1) * squareHeight(),
						curPiece.getShape());
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {

		super.paintComponent(g);
		doDrawing(g);
	}

	private void dropDown() {

		int newY = curY;

		while (newY > 0) {

			if (!tryMove(curPiece, curX, newY - 1))
				break;
			--newY;
		}

		pieceDropped();
	}

	private void oneLineDown() {

		if (!tryMove(curPiece, curX, curY - 1))
			pieceDropped();
	}

	private void clearBoard() {

		for (int i = 0; i < BoardHeight * BoardWidth; ++i)
			board[i] = Tetrominoes.NoShape;
	}

	private void pieceDropped() {

		for (int i = 0; i < 4; ++i) {

			int x = curX + curPiece.x(i);
			int y = curY - curPiece.y(i);
			board[(y * BoardWidth) + x] = curPiece.getShape();
		}

		removeFullLines();

		if (!isFallingFinished)
			newPiece();
	}

	private void newPiece() {

		curPiece.setRandomShape();
		curX = BoardWidth / 2 + 1;
		curY = BoardHeight - 1 + curPiece.minY();

		if (!tryMove(curPiece, curX, curY)) {

			curPiece.setShape(Tetrominoes.NoShape);
			timer.stop();
			isStarted = false;
			statusbar.setText("game over");
		}
	}

	private boolean tryMove(Shape newPiece, int newX, int newY) {

		for (int i = 0; i < 4; ++i) {

			int x = newX + newPiece.x(i);
			int y = newY - newPiece.y(i);

			if (x < 0 || x >= BoardWidth || y < 0 || y >= BoardHeight)
				return false;

			if (shapeAt(x, y) != Tetrominoes.NoShape)
				return false;
		}

		curPiece = newPiece;
		curX = newX;
		curY = newY;

		repaint();

		return true;
	}

	private void removeFullLines() {

		int numFullLines = 0;

		for (int i = BoardHeight - 1; i >= 0; --i) {
			boolean lineIsFull = true;

			for (int j = 0; j < BoardWidth; ++j) {
				if (shapeAt(j, i) == Tetrominoes.NoShape) {
					lineIsFull = false;
					break;
				}
			}

			if (lineIsFull) {
				++numFullLines;
				for (int k = i; k < BoardHeight - 1; ++k) {
					for (int j = 0; j < BoardWidth; ++j)
						board[(k * BoardWidth) + j] = shapeAt(j, k + 1);
				}
			}
		}

		if (numFullLines > 0) {

			numLinesRemoved += numFullLines;
			statusbar.setText(String.valueOf(numLinesRemoved));
			isFallingFinished = true;
			curPiece.setShape(Tetrominoes.NoShape);
			repaint();
		}
	}

	private void drawSquare(Graphics g, int x, int y, Tetrominoes shape) {

		Color colors[] = { new Color(0, 0, 0), new Color(204, 102, 102),
				new Color(102, 204, 102), new Color(102, 102, 204),
				new Color(204, 204, 102), new Color(204, 102, 204),
				new Color(102, 204, 204), new Color(218, 170, 0) };

		Color color = colors[shape.ordinal()];

		g.setColor(color);
		g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);

		g.setColor(color.brighter());
		g.drawLine(x, y + squareHeight() - 1, x, y);
		g.drawLine(x, y, x + squareWidth() - 1, y);

		g.setColor(color.darker());
		g.drawLine(x + 1, y + squareHeight() - 1, x + squareWidth() - 1, y
				+ squareHeight() - 1);
		g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1, x
				+ squareWidth() - 1, y + 1);

	}

	class TAdapter extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {

			if (!isStarted || curPiece.getShape() == Tetrominoes.NoShape) {
				return;
			}

			int keycode = e.getKeyCode();

			if (keycode == 'p' || keycode == 'P') {
				pause();
				return;
			}

			if (isPaused)
				return;

			switch (keycode) {

			case KeyEvent.VK_LEFT:
				tryMove(curPiece, curX - 1, curY);
				break;

			case KeyEvent.VK_RIGHT:
				tryMove(curPiece, curX + 1, curY);
				break;

			case KeyEvent.VK_DOWN:
				//tryMove(curPiece.rotateRight(), curX, curY);
				dropDown();
				break;

			case KeyEvent.VK_UP:
				//tryMove(curPiece.rotateLeft(), curX, curY);
				tryMove(curPiece.rotateRight(), curX, curY);
				break;

			case KeyEvent.VK_SPACE:
				dropDown();
				break;

			case 'd':
				oneLineDown();
				break;

			case 'D':
				oneLineDown();
				break;
			}
		}
	}
}