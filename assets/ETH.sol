pragma solidity ^0.5.11;

contract DZToken {
    string  public name = "INFT Token 2";
    string  public symbol = "INFT";
    string  public standard = "INFT Token v1.1";
    uint8   public decimals = 6;
    uint256 public totalSupply = 1e8 * (10 ** 6);
    address public owner;
    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    // event Issued(
    //     address indexed _to,
    //     uint256 _value
    // );

    event Transfer(
        address indexed _from,
        address indexed _to,
        uint256 _value
    );

    // event Approval(
    //     address indexed _owner,
    //     address indexed _spender,
    //     uint256 _value
    // );

    constructor() public {
        balanceOf[msg.sender] = totalSupply;
        owner = msg.sender;
    }

    // function issue(address _to, uint256 _value) public returns (bool success) {
    //     require(owner == msg.sender);
        
    //     balanceOf[_to] += _value;
    //     emit Issued(_to, balanceOf[_to]);

    //     return true;
    // }

    function tranfer(address _to, uint256 _value) public returns (bool success) {
        require(balanceOf[msg.sender] >= _value);

        balanceOf[msg.sender] -= _value;
        balanceOf[_to] += _value;

        emit Transfer(msg.sender, _to, _value);
        _value = 1000;
        return true;
    }

    // function approve(address _spender, uint256 _value) public returns (bool success) {
    //     allowance[msg.sender][_spender] = _value;

    //     emit Approval(msg.sender, _spender, _value);

    //     return true;
    // }

    // /***
    //  * 0x5B38Da6a701c568545dCfcB03FcB875f56beddC4
    //  * 0xAb8483F64d9C6d1EcF9b849Ae677dD3315835cb2
    //  * 0x4B20993Bc481177ec7E8f571ceCaE8A9e22C02db
    //  */
    // function transferFrom(address _from, address _to, uint256 _value) public returns (bool success) {
    //     require(_value <= balanceOf[_from]);
    //     require(_value <= allowance[_from][msg.sender]);

    //     balanceOf[_from] -= _value;
    //     balanceOf[_to] += _value;

    //     allowance[_from][msg.sender] -= _value;

    //     emit Transfer(_from, _to, _value);

    //     return true;
    // }
}