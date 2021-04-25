package chess.dao;

import chess.domain.Side;
import chess.domain.board.Board;
import chess.domain.piece.Piece;
import chess.domain.piece.PieceFactory;
import chess.domain.position.Position;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class SpringBoardDao {

    private static final String COMMA = ",";
    private static final String WHITE_INITIAL = "W";
    private static final String BLACK_INITIAL = "B";

    private final JdbcTemplate jdbcTemplate;

    public SpringBoardDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Board initBoard(String roomName) {
        return findBoard(roomName).orElseGet(() -> {
            newBoard(roomName);
            return initBoard(roomName);
        });
    }

    public Map<Position, Piece> newBoard(String roomName) {
        String query = "INSERT INTO board (roomName, position, pieceName, turn) VALUES (?, ?, ?, ?)";
        Board board = Board.getGamingBoard();
        this.jdbcTemplate.update(query, roomName, boardPositionSet(board.getBoard()), boardPieceSet(board.getBoard()), "WHITE");

        return board.getBoard();
    }

    public void updateBoard(Board board, String turn, String roomName) {
        String query = "UPDATE board SET position = ?, pieceName = ?, turn = ? WHERE roomName = ?";
        this.jdbcTemplate.update(query, boardPositionSet(board.getBoard()), boardPieceSet(board.getBoard()), turn, roomName);
    }

    public Optional<Board> findBoard(String roomName) {
        String query = "select * from board where roomName=?";
        return this.jdbcTemplate.query(
                query,
                (resultSet, rowNum) -> daoToBoard(
                        resultSet.getString("position"),
                        resultSet.getString("pieceName")
                ),
                roomName).stream().findAny().map(Board::new);
    }

    public Optional<Side> findTurn(String roomName) {
        String query = "SELECT turn FROM board WHERE roomName = ?";
        return jdbcTemplate.queryForList(query, String.class, roomName)
                .stream()
                .findAny()
                .map(Side::getTurnByName);
    }

    private String boardPositionSet(Map<Position, Piece> board) {
        return board.keySet()
                .stream()
                .map(Position::positionName)
                .collect(Collectors.joining(COMMA));
    }

    private String boardPieceSet(Map<Position, Piece> board) {
        List<String> pieceNames = new ArrayList<>();
        for (Piece piece : board.values()) {
            pieceNames.add(pieceToName(piece));
        }
        return String.join(COMMA, pieceNames);
    }

    private String pieceToName(Piece piece) {
        String pieceName = piece.getInitial();
        if (piece.side() == Side.WHITE) {
            return WHITE_INITIAL + pieceName.toUpperCase();
        }
        if (piece.side() == Side.BLACK) {
            return BLACK_INITIAL + pieceName.toUpperCase();
        }
        return pieceName;
    }

    private Map<Position, Piece> daoToBoard(String positions, String pieces) {
        Map<Position, Piece> board = new LinkedHashMap<>();

        String[] position = positions.split(COMMA);
        String[] piece = pieces.split(COMMA);

        for (int i = 0; i < position.length; i++) {
            board.put(Position.from(position[i]), PieceFactory.createPieceByName(piece[i]));
        }
        return board;
    }

    public boolean checkDuplicateByRoomName(String roomName) {
        String query = "SELECT count(*) FROM board WHERE roomName = ?";
        int count = this.jdbcTemplate.queryForObject(query, Integer.class, roomName);
        return count != 0;
    }

    public List<String> findRooms() {
        String query = "SELECT roomName FROM board";
        return this.jdbcTemplate.query(query,
                (resultSet, rowNum) -> resultSet.getString("roomName"));
    }

    public void deleteRoom(String roomName) {
        String query = "DELETE FROM board WHERE roomName = ?";
        this.jdbcTemplate.update(query, roomName);
    }
}
