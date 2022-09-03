import { useState } from "react";
import AppBar from "@mui/material/AppBar";
import Container from "@mui/material/Container";
import Toolbar from "@mui/material/Toolbar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import Menu from "@mui/material/Menu";
import MenuIcon from "@mui/icons-material/Menu";
import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";

const pages = ["Portfolio", "Transactions"];

const Brand = ({ mobile }) => (
  <Typography
    variant="h6"
    noWrap
    sx={{
      mr: 2,
      display: mobile ? { xs: "flex", md: "none" } : { xs: "none", md: "flex" },
      fontWeight: 200,
      letterSpacing: ".1rem",
      flexGrow: mobile ? 1 : 0,
    }}
  >
    Investments
  </Typography>
);

const MobileMenu = () => {
  const [anchor, setAnchor] = useState(null);

  return (
    <Box sx={{ flexGrow: 1, display: { xs: "flex", md: "none" } }}>
      <IconButton size="large" onClick={(e) => setAnchor(e.currentTarget)}>
        <MenuIcon />
      </IconButton>
      <Menu
        anchorEl={anchor}
        keepMounted
        open={Boolean(anchor)}
        onClose={() => setAnchor(null)}
        sx={{ display: { xs: "block", md: "none" } }}
      >
        {pages.map((page) => (
          <MenuItem key={page} onClick={() => setAnchor(null)}>
            <Typography>{page}</Typography>
          </MenuItem>
        ))}
      </Menu>
    </Box>
  );
};

const DesktopMenu = () => (
  <Box sx={{ flexGrow: 1, display: { xs: "none", md: "flex" } }}>
    {pages.map((page) => (
      <Button key={page} sx={{ color: "white" }}>
        {page}
      </Button>
    ))}
  </Box>
);

const NavBar = () => (
  <AppBar position="static">
    <Container maxWidth="xl">
      <Toolbar disableGutters>
        <Brand mobile={false} />
        <MobileMenu />
        <Brand mobile={true} />
        <DesktopMenu />
      </Toolbar>
    </Container>
  </AppBar>
);
export default NavBar;
