import { useState } from "react";
import { gql, useQuery, useMutation } from "@apollo/client";
import {
  Button,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Typography,
  TextField,
  IconButton,
} from "@material-ui/core";
import CreditCardIcon from "@material-ui/icons/CreditCard";
import DeleteIcon from "@material-ui/icons/Delete";
import { makeStyles, Theme } from "@material-ui/core/styles";
import { grey } from "@material-ui/core/colors";

const useStyles = makeStyles((theme: Theme) => ({
  container: {
    width: "100%",
    maxWidth: "none",
  },
  header: {
    display: "flex",
    alignItems: "center",
    marginBottom: "20px",
  },
  listItem: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    backgroundColor: "white",
    borderRadius: "8px",
    margin: "12px 0",
    padding: "16px",
    boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
  },
  icon: {
    marginRight: "20px",
    fontSize: "28px",
    color: theme.palette.primary.main,
  },
  primaryText: {
    fontWeight: "bold",
    color: theme.palette.text.primary,
  },
  activeText: {
    color: theme.palette.success.main,
  },
  inactiveText: {
    color: theme.palette.error.main,
  },
  button: {
    backgroundColor: theme.palette.action.hover,
    color: theme.palette.text.primary,
    padding: "6px 12px",
    boxShadow: "0 1px 2px rgba(0,0,0,0.1)",
    transition: "all 0.3s ease",
    "&:hover": {
      backgroundColor: theme.palette.action.selected,
    },
  },
  addMethodForm: {
    display: "flex",
    marginBottom: "20px",
  },
  addMethodInput: {
    flexGrow: 1,
    marginRight: "10px",
    backgroundColor: "white",
  },
  deleteButton: {
    color: theme.palette.error.main,
  },
}));

export const GET_PAYMENT_METHODS = gql`
  query GetPaymentMethods($parentId: Long!) {
    paymentMethods(parentId: $parentId) {
      id
      method
      isActive
      createdAt
    }
  }
`;

export const SET_ACTIVE_PAYMENT_METHOD = gql`
  mutation SetActivePaymentMethod($parentId: Long!, $methodId: Long!) {
    setActivePaymentMethod(parentId: $parentId, methodId: $methodId) {
      id
      method
      isActive
    }
  }
`;

const ADD_PAYMENT_METHOD = gql`
  mutation AddPaymentMethod($parentId: Long!, $method: String!, $createdAt: String!) {
    addPaymentMethod(parentId: $parentId, method: $method, createdAt: $createdAt) {
      id
      method
      isActive
      createdAt
    }
  }
`;

const DELETE_PAYMENT_METHOD = gql`
  mutation DeletePaymentMethod($parentId: Long!, $methodId: Long!) {
    deletePaymentMethod(parentId: $parentId, methodId: $methodId)
  }
`;

const PaymentMethods = ({ parentId }: { parentId: number }) => {
  const classes = useStyles();
  const [newMethod, setNewMethod] = useState("");
  const { loading, data } = useQuery(GET_PAYMENT_METHODS, {
    variables: { parentId },
  });
  const [setActivePaymentMethod] = useMutation(SET_ACTIVE_PAYMENT_METHOD);
  const [addPaymentMethod] = useMutation(ADD_PAYMENT_METHOD);
  const [deletePaymentMethod] = useMutation(DELETE_PAYMENT_METHOD);

  if (loading) return <p>Loading...</p>;

  const handleActivate = (methodId: number) => {
    setActivePaymentMethod({
      variables: { parentId, methodId },
      refetchQueries: [{ query: GET_PAYMENT_METHODS, variables: { parentId } }],
    });
  };

  const getFormattedDateTime = (): string => {
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');

    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
           `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
  };

  const handleAddMethod = (e: React.FormEvent) => {
    const createdAt = getFormattedDateTime();

    e.preventDefault();
    if (newMethod.trim()) {
      addPaymentMethod({
        variables: { parentId, method: newMethod.trim(), createdAt },
        refetchQueries: [{ query: GET_PAYMENT_METHODS, variables: { parentId } }],
      }).then(() => {
        setNewMethod("");
      });
    }
  };

  const handleDeleteMethod = (methodId: number, methodIsActive: boolean) => {
    if (methodIsActive) {
      // Check if there are other payment methods available
      const otherMethods = data.paymentMethods.filter((method: any) => method.id !== methodId);

      if (otherMethods.length === 0) {
        // If no other methods are available, prevent deletion
        alert("You cannot delete the only active payment method. Please add a new payment method first.");
        return;
      } else {
        // If there are other methods, set the first available one as active before deleting
        handleActivate(otherMethods[0].id)
          // After setting the new active method, delete the original method
          deletePaymentMethod({
            variables: { parentId, methodId },
            refetchQueries: [{ query: GET_PAYMENT_METHODS, variables: { parentId } }],
          });
      }
    } else {
      // If the method is not active, just delete it
      deletePaymentMethod({
        variables: { parentId, methodId },
        refetchQueries: [{ query: GET_PAYMENT_METHODS, variables: { parentId } }],
      });
    }
  };

  return (
    <div className={classes.container}>
      <div className={classes.header}>
        <div>
          <Typography variant="h5">Payment Methods</Typography>
          <Typography variant="subtitle1" style={{ color: grey[700] }}>
            Manage and select your preferred payment options
          </Typography>
        </div>
      </div>
      <form onSubmit={handleAddMethod} className={classes.addMethodForm}>
        <TextField
          className={classes.addMethodInput}
          value={newMethod}
          onChange={(e) => setNewMethod(e.target.value)}
          placeholder="Add new payment method"
          variant="outlined"
          size="small"
        />
        <Button
          type="submit"
          variant="contained"
          className={classes.button}
          size="small"
        >
          Add Method
        </Button>
      </form>
      <List>
        {data?.paymentMethods.map((method: any) => (
          <ListItem key={method.id} className={classes.listItem}>
            <ListItemIcon>
              <CreditCardIcon />
            </ListItemIcon>
            <ListItemText
              primary={method.method}
              secondary={`${method.isActive ? "Active" : "Inactive"} • Created on: ${new Date(method.createdAt).toLocaleDateString()}`}
              primaryTypographyProps={{ className: classes.primaryText }}
              secondaryTypographyProps={{
                className: method.isActive
                  ? classes.activeText
                  : classes.inactiveText,
              }}
            />
            {!method.isActive && (
              <Button
                variant="contained"
                className={classes.button}
                onClick={() => handleActivate(method.id)}
                size="small"
              >
                Activate
              </Button>
            )}
            <IconButton
              className={classes.deleteButton}
              onClick={() => handleDeleteMethod(method.id, method.isActive)}
              size="small"
            >
              <DeleteIcon />
            </IconButton>
          </ListItem>
        ))}
      </List>
    </div>
  );
};

export default PaymentMethods;
